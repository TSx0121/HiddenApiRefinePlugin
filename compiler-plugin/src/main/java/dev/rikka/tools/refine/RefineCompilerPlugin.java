package dev.rikka.tools.refine;

import com.google.auto.service.AutoService;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@AutoService(Plugin.class)
public class RefineCompilerPlugin implements Plugin {
    private Symtab symtab;
    private Names names;

    private Symbol.ClassSymbol refineFor;
    private Symbol.ClassSymbol refineName;
    private Symbol.ClassSymbol refineUse;

    private static <T> T singleOrNull(Iterable<T> iterable) {
        final Iterator<T> iterator = iterable.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    private static Attribute resolveAnnotationValue(Symbol type, Symbol.ClassSymbol annotation) {
        if (annotation == null) return null;

        for (Attribute.Compound compound : type.getAnnotationMirrors()) {
            if (compound.type.tsym != annotation) {
                continue;
            }

            for (Pair<Symbol.MethodSymbol, Attribute> value : compound.values) {
                if (value.fst.name.contentEquals("value")) {
                    return value.snd;
                }
            }
        }

        return null;
    }

    private Scope.WriteableScope duplicateAndMergeSymbols(
            List<Pair<Symbol, Name>> renames,
            Scope.WriteableScope source,
            Iterable<Symbol> extra,
            Symbol owner
    ) {
        Scope.WriteableScope result = source.dupUnshared(owner);

        for (Symbol member : extra) {
            if (member instanceof Symbol.MethodSymbol || member instanceof Symbol.VarSymbol) {
                Symbol duplicated = member.clone(owner);

                final Attribute refineNameValue = resolveAnnotationValue(member, refineName);
                if (refineNameValue instanceof Attribute.Constant) {
                    renames.add(new Pair<>(duplicated, duplicated.name.table.fromString(((Attribute.Constant) refineNameValue).value.toString())));
                }

                result.enter(duplicated);
            } else if (member instanceof Symbol.ClassSymbol) {
                if (resolveAnnotationValue(member, refineFor) != null) {
                    continue;
                }

                final Symbol.ClassSymbol from = (Symbol.ClassSymbol) member;
                final Symbol.ClassSymbol clazz = new Symbol.ClassSymbol(from.flags_field, from.name, owner);

                clazz.completer = Symbol.Completer.NULL_COMPLETER;

                final Symbol.ClassSymbol origin = singleOrNull(symtab.getClassesForName(clazz.flatname));
                if (origin != null) {
                    clazz.members_field = duplicateAndMergeSymbols(renames, origin.members_field, from.members_field.getSymbols(), owner);
                    clazz.permitted = origin.permitted;
                    clazz.setAnnotationTypeMetadata(origin.getAnnotationTypeMetadata());
                } else {
                    clazz.members_field = duplicateAndMergeSymbols(renames, Scope.WriteableScope.create(clazz), from.members_field.getSymbols(), owner);
                    clazz.permitted = from.permitted;
                    clazz.setAnnotationTypeMetadata(from.getAnnotationTypeMetadata());
                }

                result.enter(clazz);
            }
        }

        return result;
    }

    @Override
    public String getName() {
        return "HiddenApiRefine";
    }

    @Override
    public void init(JavacTask task, String... args) {
        System.out.println(Arrays.asList(args));

        final Context context = ((BasicJavacTask) task).getContext();

        symtab = Symtab.instance(context);
        names = Names.instance(context);

        task.addTaskListener(new TaskListener() {
            private final ArrayList<Pair<Symbol.ClassSymbol, Scope.WriteableScope>> restores = new ArrayList<>();
            private final ArrayList<Pair<Symbol, Name>> renames = new ArrayList<>();

            private boolean symbol = false;

            @Override
            public void started(TaskEvent e) {
                try {
                    if (e.getKind() == TaskEvent.Kind.ANALYZE) {
                        if (!symbol) {
                            refineFor = singleOrNull(symtab.getClassesForName(names.fromString(RefineFor.class.getName())));
                            if (refineFor == null) {
                                task.removeTaskListener(this);
                                return;
                            }

                            refineUse = singleOrNull(symtab.getClassesForName(names.fromString(RefineUse.class.getName())));
                            if (refineUse == null) {
                                task.removeTaskListener(this);
                                return;
                            }

                            refineName = singleOrNull(symtab.getClassesForName(names.fromString(RefineName.class.getName())));

                            symbol = true;
                        }
                    }

                    if (e.getKind() == TaskEvent.Kind.ANALYZE) {
                        for (Pair<Symbol.ClassSymbol, Scope.WriteableScope> restore : restores) {
                            restore.fst.members_field = restore.snd;
                        }

                        restores.clear();
                        renames.clear();

                        for (Tree type : e.getCompilationUnit().getTypeDecls()) {
                            final Symbol.ClassSymbol compiling = ((JCTree.JCClassDecl) type).sym;

                            final Attribute uses = resolveAnnotationValue(compiling, refineUse);
                            if (!(uses instanceof Attribute.Array)) {
                                continue;
                            }

                            for (Attribute value : ((Attribute.Array) uses).values) {
                                if (!(value instanceof Attribute.Class)) {
                                    continue;
                                }

                                Symbol.ClassSymbol refineClass = (Symbol.ClassSymbol) ((Attribute.Class) value).classType.tsym;

                                final Attribute refine = resolveAnnotationValue(refineClass, refineFor);
                                if (!(refine instanceof Attribute.Class)) {
                                    continue;
                                }

                                Symbol.ClassSymbol target = (Symbol.ClassSymbol) ((Attribute.Class) refine).classType.tsym;
                                target.complete();

                                Scope.WriteableScope duplicated = duplicateAndMergeSymbols(renames, target.members_field, refineClass.members_field.getSymbols(), target);
                                restores.add(new Pair<>(target, target.members_field));
                                target.members_field = duplicated;
                            }
                        }
                    } else if (e.getKind() == TaskEvent.Kind.GENERATE) {
                        for (Pair<Symbol, Name> rename : renames) {
                            rename.fst.name = rename.snd;
                        }
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();

                    throw throwable;
                }
            }
        });
    }
}
