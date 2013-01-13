package metafunction;

import java.util.functions.Block;
import java.util.functions.Mapper;

public class MetaFunction<T> {
    private final Mapper<T, Object[]> applier;

    private MetaFunction(Mapper<T, Object[]> applier) {
        this.applier = applier;
    }

    public T apply(Object... args) {
        return applier.map(args);
    }

    public static <T> MetaFunction<T> of(Mapper<T, Object[]> applier) {
        return new MetaFunction<T>(applier);
    }
}
