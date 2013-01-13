package metafunction;

import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;

public class MetaFunctionTest {

    static class FunctionApplier extends FunctionApplier_MetaFunction<Object> {
        MetaFunction<Object> function;

        @MetaMethod void when(MetaFunction<Object> function) {
            this.function = function;
        }

        public Object apply(Object... args) {
            return function.apply(args);
        }
    }

    static class FunctionAssert extends FunctionAssert_MetaFunction<List<? extends Object>> {


        @MetaMethod AssertBuilder whenFunction(MetaFunction<List<? extends Object>> function) {
            return new AssertBuilder(function, this);
        }

        public static class AssertBuilder {
            final MetaFunction<List<? extends Object>> currentFunction;
            final FunctionAssert functionAssert;

            AssertBuilder(MetaFunction<List<? extends Object>> currentFunction, FunctionAssert functionAssert) {
                this.currentFunction = currentFunction;
                this.functionAssert = functionAssert;
            }

            public Assertions calledWith(Object... parameters) {
                return new Assertions(functionAssert, currentFunction, parameters);
            }
        }

        public static class Assertions {
            final FunctionAssert functionAssert;
            final MetaFunction<List<? extends Object>> currentFunction;
            final Object[] currentParams;
            Assertions(FunctionAssert functionAssert, MetaFunction<List<? extends Object>> currentFunction, Object[] currentParams) {
                this.functionAssert = functionAssert;
                this.currentFunction = currentFunction;
                this.currentParams = currentParams;
            }

            public FunctionAssert assertReturns(Object... returns) {
                List<?> result = currentFunction.apply(currentParams);
                Assert.assertEquals(Arrays.asList(returns), result);
                return functionAssert;
            }

            public FunctionAssert assertReturnsArguments() {
                assertReturns(currentParams);
                return functionAssert;
            }
        }

    }

    @Test public void testMetaFunction() {
        FunctionApplier fn = new FunctionApplier();

        fn.when(() -> "yo");
        Assert.assertEquals("yo", fn.apply());

        fn.when((String s) -> s);
        Assert.assertEquals("yo", fn.apply("yo"));
    }

    @Test public void testFluently() {
        new FunctionAssert()

                .whenFunction(() -> asList("foo"))
                .calledWith()
                .assertReturns("foo")

                .whenFunction((String s) -> asList(s))
                .calledWith("foo")
                .assertReturnsArguments()

                .whenFunction((Integer i) -> asList(i))
                .calledWith(4)
                .assertReturnsArguments()

                .whenFunction((Integer i, String s) -> asList(i, s))
                .calledWith(4, "foo")
                .assertReturnsArguments()

                .whenFunction((Integer i, String s, Object huh) -> asList(i, s, huh))
                .calledWith(4, "foo", new Object[]{})
                .assertReturnsArguments()

                .whenFunction((Integer i, String s, Object huh, String four) -> asList(i, s, huh, four))
                .calledWith(4, "foo", new Object[]{}, null)
                .assertReturnsArguments()

                .whenFunction((String a, String b, String c, String d, String e, String f, String g, String h, String i, String j) ->
                        asList(a, b, c, d, e, f, g, h, i, j))
                .calledWith("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")
                .assertReturnsArguments()
        ;
    }

    static class RouterExample extends RouterExample_MetaFunction<Object> {
        final Map<String, MetaFunction<Object>> handlers = Maps.newHashMap();

        @MetaMethod RouterExample route(String path, MetaFunction<Object> handler) {
            handlers.put(path, handler);
            return this;
        }

        public Object apply(String path, Object... args) {
            return handlers.get(path).apply(args);
        }
    }


    @Test public void testMetaMethodsCanHaveAdditionalArgs() {
        RouterExample router = new RouterExample();
        router.route("foo", () -> "bar")
              .route("name", (String name) -> "Hello, " + name);


        assertThat(router.apply("foo")).isEqualTo("bar");

        assertThat(router.apply("name", "Sam")).isEqualTo("Hello, Sam");
    }


    public static class MetaFunctionNamedArgs extends MetaFunctionNamedArgs_MetaFunction<String> {
        @MetaMethod public String go(MetaFunction<String> func, Object... args) {
            return func.apply(args);
        }
    }


    @Test public void testFunctionCanBeNamedArgsAndHaveVarArgs() {
        String result = new MetaFunctionNamedArgs().go((String arg) -> arg, "Foo");
        assertThat(result).isEqualTo("Foo");
    }







    //TODO: multiple @MetaMethods with different types?
    //TODO: ensure exactly one MetaFunction parameter per method
    //TODO: enable generic metamethod return type
    //TODO: validate clashing meta methods
    //TODO: single Object arg causes error when in same package
    //TODO: nice error messages for mismatched params
}
