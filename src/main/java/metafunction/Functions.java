package metafunction;

public class Functions {

    //set of generic functions, with increasing arity.
    //these should eventually be generated at compile time, to clean up the source and make it easier to modify
    public interface F<R> {   }
    public interface F0<R> extends F<R> { R apply(); }
    public interface F1<T1, R> extends F<R> { R apply(T1 p1); }
    public interface F2<T1, T2, R> extends F<R> { R apply(T1 p1, T2 p); }
    public interface F3<T1, T2, T3, R> extends F<R> { R apply(T1 p1, T2 p2, T3 p3); }
    public interface F4<T1, T2, T3, T4, R> extends F<R> { R apply(T1 p1, T2 p2, T3 p3, T4 p4); }
    public interface F5<T1, T2, T3, T4, T5, R> extends F<R> { R apply(T1 p1, T2 p2, T3 p3, T4 p4, T5 p5); }
    public interface F6<T1, T2, T3, T4, T5, T6, R> extends F<R> { R apply(T1 p1, T2 p2, T3 p3, T4 p4, T5 p5, T6 pc); }
    public interface F7<T1, T2, T3, T4, T5, T6, T7, R> extends F<R> { R apply(T1 p1, T2 p2, T3 p3, T4 p4, T5 p5, T6 pc, T7 p7); }
    public interface F8<T1, T2, T3, T4, T5, T6, T7, T8, R> extends F<R> { R apply(T1 p1, T2 p2, T3 p3, T4 p4, T5 p5, T6 pc, T7 p7, T8 p8); }
    public interface F9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> extends F<R> { R apply(T1 p1, T2 p2, T3 p3, T4 p4, T5 p5, T6 pc, T7 p7, T8 p8, T9 p9); }
    public interface F10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> extends F<R> { R apply(T1 p1, T2 p2, T3 p3, T4 p4, T5 p5, T6 pc, T7 p7, T8 p8, T9 p9, T10 p10); }

}
