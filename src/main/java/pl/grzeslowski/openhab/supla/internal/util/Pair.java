package pl.grzeslowski.openhab.supla.internal.util;

/** Simple generic pair to replace external tuple dependency. */
public record Pair<A, B>(A value0, B value1) {
    public static <A, B> Pair<A, B> with(A value0, B value1) {
        return new Pair<>(value0, value1);
    }

    public A getValue0() {
        return value0;
    }

    public B getValue1() {
        return value1;
    }
}
