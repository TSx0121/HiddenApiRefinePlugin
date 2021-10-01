package dev.rikka.tools.refine;

import com.google.auto.service.AutoService;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.*;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;

@AutoService(Plugin.class)
public class RefinePlugin implements Plugin {
    private static String resolveAnnotationValue(Symbol type, ClassSymbol annotation) {
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

    @Override
    public String getName() {
        return "HiddenApiRefine";
    }

    @Override
    public void init(JavacTask task, String... args) {
        final Context context = ((BasicJavacTask) task).getContext();

        task.addTaskListener(new TaskListener() {
            private final HashMap<Symbol, Name> renames = new HashMap<>();

            @Override
            public void started(TaskEvent e) {
                if (e.getKind() == TaskEvent.Kind.ANALYZE) {
                    final Symtab symtab = Symtab.instance(context);

                    final HashMap<String, ClassSymbol> classes = new HashMap<>();
                    for (ClassSymbol clazz : symtab.getAllClasses()) {
                        classes.put(clazz.className(), clazz);
                    }

                    final ClassSymbol refineFor = classes.get(RefineFor.class.getName());
                    if (refineFor == null) {
                        return;
                    }

                    final ClassSymbol refineName = classes.get(RefineName.class.getName());

                    for (ClassSymbol clazz : classes.values()) {
                        final String refineForTarget = resolveAnnotationValue(clazz, refineFor);
                        if (refineForTarget == null)
                            continue;

                        final ClassSymbol target = classes.get(refineForTarget);
                        if (target == null)
                            continue;

                        final Scope.WriteableScope scope = target.members();

                        for (Symbol symbol : clazz.members().getSymbols()) {
                            final Symbol duplicated = symbol.clone(scope.owner);

                            final String refineNameValue = resolveAnnotationValue(symbol, refineName);
                            if (refineNameValue != null) {
                                renames.put(duplicated, symbol.name);

                                duplicated.name = duplicated.name.table.fromString(refineNameValue);
                            }

                            scope.enter(duplicated);
                        }
                    }
                }
            }

            @Override
            public void finished(TaskEvent e) {
                try {
                    if (e.getKind() == TaskEvent.Kind.ANALYZE) {
                        final Symtab symtab = Symtab.instance(context);
                        final ArrayList<ClassSymbol> classes = new ArrayList<>();

                        symtab.getAllClasses().forEach(classes::add);

                        for (ClassSymbol clazz : classes) {
                            for (Symbol symbol : clazz.members().getSymbols()) {
//                                if (symbol instanceof Symbol.MethodSymbol) {
//                                    if (((Symbol.MethodSymbol) symbol).code != null) {
//                                        System.out.println(symbol);
//                                    }
//                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();

                    throw t;
                }
            }
        });
    }

    @Override
    public boolean autoStart() {
        return true;
    }
}
