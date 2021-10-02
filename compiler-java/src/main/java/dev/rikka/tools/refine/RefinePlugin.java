package dev.rikka.tools.refine;

import com.google.auto.service.AutoService;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;

@AutoService(Plugin.class)
public class RefinePlugin implements Plugin {
    private final ArrayList<Pair<Symbol, Pair<Name, Name>>> renames = new ArrayList<>();
    private final HashMap<String, Symbol.ClassSymbol> classes = new HashMap<>();

    private Symtab symtab;
    private Symbol.ClassSymbol refineFor;
    private Symbol.ClassSymbol refineName;

    private static String resolveAnnotationValue(Symbol type, Symbol.ClassSymbol annotation) {
        if (annotation == null) return null;

        for (Attribute.Compound compound : type.getAnnotationMirrors()) {
            if (compound.type.tsym != annotation) {
                continue;
            }

            for (Pair<Symbol.MethodSymbol, Attribute> value : compound.values) {
                if (value.fst.name.contentEquals("value")) {
                    return value.snd.getValue().toString();
                }
            }
        }

        return null;
    }

    private void duplicateMembersTo(Symbol.ClassSymbol from, Symbol.ClassSymbol to) {
        final Scope.WriteableScope scope = to.members();

        for (Symbol symbol : from.members().getSymbols()) {
            if (symbol instanceof Symbol.MethodSymbol || symbol instanceof Symbol.VarSymbol) {
                final Symbol duplicated = symbol.clone(to);

                final String refineNameValue = resolveAnnotationValue(symbol, refineName);
                if (refineNameValue != null) {
                    renames.add(new Pair<>(duplicated, new Pair<>(duplicated.name, duplicated.name.table.fromString(refineNameValue))));
                }

                scope.enter(duplicated);
            } else if (symbol instanceof Symbol.ClassSymbol) {
                if (resolveAnnotationValue(symbol, refineFor) != null)
                    continue;

                Symbol.ClassSymbol clazz = symtab.defineClass(symbol.name, to);

                clazz.completer = Symbol.Completer.NULL_COMPLETER;
                clazz.flags_field = symbol.flags_field;
                clazz.members_field = Scope.WriteableScope.create(clazz);

                duplicateMembersTo((Symbol.ClassSymbol) symbol, clazz);

                scope.enter(clazz);
            }
        }
    }

    @Override
    public String getName() {
        return "HiddenApiRefine";
    }

    @Override
    public void init(JavacTask task, String... args) {
        final Context context = ((BasicJavacTask) task).getContext();

        symtab = Symtab.instance(context);

        task.addTaskListener(new TaskListener() {
            private boolean analyzed = false;

            @Override
            public void started(TaskEvent e) {
                try {
                    if (e.getKind() == TaskEvent.Kind.ANALYZE) {
                        if (!analyzed) {
                            analyzed = true;

                            for (Symbol.ClassSymbol clazz : symtab.getAllClasses()) {
                                classes.put(clazz.className(), clazz);
                            }

                            refineFor = classes.get(RefineFor.class.getName());
                            if (refineFor == null) {
                                return;
                            }

                            refineName = classes.get(RefineName.class.getName());

                            for (Symbol.ClassSymbol clazz : classes.values()) {
                                final String refineForTarget = resolveAnnotationValue(clazz, refineFor);
                                if (refineForTarget == null)
                                    continue;

                                final Symbol.ClassSymbol target = classes.get(refineForTarget);
                                if (target == null)
                                    continue;

                                duplicateMembersTo(clazz, target);
                            }
                        }
                    }

                    if (e.getKind() == TaskEvent.Kind.ANALYZE) { // restore name
                        for (Pair<Symbol, Pair<Name, Name>> rename : renames) {
                            rename.fst.name = rename.snd.fst;
                        }
                    } else if (e.getKind() == TaskEvent.Kind.GENERATE) { // patch name
                        for (Pair<Symbol, Pair<Name, Name>> rename : renames) {
                            rename.fst.name = rename.snd.snd;
                        }
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();

                    throw throwable;
                }
            }
        });
    }

    @Override
    public boolean autoStart() {
        return true;
    }
}
