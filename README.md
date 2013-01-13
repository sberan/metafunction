

# MetaFunction

Uses annotation processing to create functions which accept lambdas with unknown Arity (up to 10 parameters)

# Usage:

### Defining the MetaMethod:

    public class FunctionApplier extends FunctionApplier_MetaFunction<String> {
        @MetaMethod public void execute(MetaFunction<String> function, String... args) {
             System.out.println(function.apply(args));
        }
    }

Note: FunctionApplier_MetaFunction will be generated automatically by jsr-269 at build time.

### Calling the MetaMethod:

    new FunctionApplier().execute(() -> "Hello");
    //prints "Hello"

    new FunctionApplier().execute((String name) -> "Hello" + name, "World");
    //prints "Hello World"
