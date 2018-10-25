package com.microsoft.bot.builder;

import com.microsoft.bot.builder.adapters.TestAdapter;
import com.microsoft.bot.builder.adapters.TestFlow;
import com.microsoft.bot.schema.ActivityImpl;
import com.microsoft.bot.schema.models.Activity;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CatchException_MiddlewareTest {

    @Test
    public void CatchException_TestMiddleware_TestStackedErrorMiddleware() throws ExecutionException, InterruptedException {

        TestAdapter adapter = new TestAdapter()
                .Use(new CatchExceptionMiddleware<Exception>(new CallOnException() {
                    @Override
                    public <T> void apply(TurnContext context, T t) throws Exception {
                        return CompletableFuture.runAsync(() -> {
                            Activity activity = context.activity();
                            if (activity instanceof ActivityImpl) {
                                try {
                                    context.SendActivityAsync(((ActivityImpl) activity).CreateReply(t.toString()));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    throw new RuntimeException(String.format("CatchException_TestMiddleware_TestStackedErrorMiddleware:SendActivityAsync failed %s", e.toString()));
                                }
                            } else
                                Assert.assertTrue("Test was built for ActivityImpl", false);

                        });

                    }
                }, Exception.class))
                // Add middleware to catch NullReferenceExceptions before throwing up to the general exception instance
                .Use(new CatchExceptionMiddleware<NullPointerException>(new CallOnException() {
                    @Override
                    public <T> void apply(TurnContext context, T t) throws Exception {
                        context.SendActivityAsync("Sorry - Null Reference Exception");
                        return CompletableFuture.completedFuture(null);
                    }
                }, NullPointerException.class));


        new TestFlow(adapter, (context) ->
                {

                    if (context.getActivity().text() == "foo") {
                        try {
                            context.SendActivity(context.getActivity().text());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (context.getActivity().text() == "UnsupportedOperationException") {
                        throw new UnsupportedOperationException("Test");
                    }

                }
        )
                .Send("foo")
                .AssertReply("foo", "passthrough")
                .Send("UnsupportedOperationException")
                .AssertReply("Test")
                .StartTest();

    }

/*        @Test
        // [TestCategory("Middleware")]
    public void CatchException_TestMiddleware_SpecificExceptionType()
{
    TestAdapter adapter = new TestAdapter()
            .Use(new CatchExceptionMiddleware<Exception>((context, exception) =>
            {
                    context.SendActivityAsync("Generic Exception Caught");
    return CompletableFuture.CompletedTask;
                }))
                .Use(new CatchExceptionMiddleware<NullReferenceException>((context, exception) =>
        {
                context.SendActivityAsync(exception.Message);
    return CompletableFuture.CompletedTask;
                }));


    await new TestFlow(adapter, (context) =>
        {
    if (context.Activity.AsMessageActivity().Text == "foo")
    {
        context.SendActivityAsync(context.Activity.AsMessageActivity().Text);
    }

    if (context.Activity.AsMessageActivity().Text == "NullReferenceException")
    {
        throw new NullReferenceException("Test");
    }

    return CompletableFuture.CompletedTask;
                })
                .Send("foo")
        .AssertReply("foo", "passthrough")
        .Send("NullReferenceException")
        .AssertReply("Test")
        .StartTest();
}*/
}