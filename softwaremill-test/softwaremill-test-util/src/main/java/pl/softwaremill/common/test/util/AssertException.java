package pl.softwaremill.common.test.util;

import org.testng.Assert;

import static pl.softwaremill.common.test.util.AssertException.ExceptionMatch.EXCEPTION_CLASS_MUST_EQUAL;

/**
 * Allows expecting and intercepting exceptions in a nice way.
 * Use it to intercept exceptions in your tests, in a way that allows
 * sticking to the given/when/then flow, and validate exception throws on
 * <p/>
 * SoftwareBirr 02.2012
 *
 * @author Konrad Malawski (konrad.malawski@java.pl)
 */
public class AssertException {

    public static abstract class ExceptionMatch {

        public static final ExceptionMatch.Strategy EXCEPTION_CLASS_MUST_EQUAL = new Strategy() {
            @Override
            public boolean matchesExpected(Class<? extends Throwable> expectedClass, Throwable got, String expectedMessage) {
                return got.getClass().equals(expectedClass);
            }

            public void failWithExpectedButGot(Class<? extends Throwable> expectedClass, Throwable got, String expectedMessage) {
                Assert.fail(String.format("Expected [%s] to be thrown but got [%s]", expectedClass.getSimpleName(), got.getClass().getSimpleName()));
            }
        };

        /**
         * Please use EXCEPTION_CLASS_MUST_EQUAL instead
         */
        @Deprecated
        public static final ExceptionMatch.Strategy EXCEPTION_MUST_EQUAL = EXCEPTION_CLASS_MUST_EQUAL;

        public static final ExceptionMatch.Strategy EXCEPTION_MAY_BE_SUBCLASS_OF = new Strategy() {
            @Override
            public boolean matchesExpected(Class<? extends Throwable> expectedClass, Throwable got, String expectedMessage) {
                return expectedClass.isAssignableFrom(got.getClass());
            }

            public void failWithExpectedButGot(Class<? extends Throwable> expectedClass, Throwable got,String expectedMessage) {
                Assert.fail(String.format("Expected subclass of [%s] to be thrown but got [%s]", expectedClass.getSimpleName(), got.getClass().getSimpleName()));
            }
        };

        public static final ExceptionMatch.Strategy EXCEPTION_CLASS_AND_MESSAGE_MUST_EQUAL = new Strategy() {
            @Override
            public boolean matchesExpected(Class<? extends Throwable> expectedClass, Throwable got, String expectedMessage) {
                return got.getClass().equals(expectedClass) && expectedMessage.equals(got.getMessage());
            }

            public void failWithExpectedButGot(Class<? extends Throwable> expectedClass, Throwable got, String expectedMessage) {
                Assert.fail(String.format("Expected [%s] to be thrown with message [%s] but got [%s] with message [%s]", expectedClass.getSimpleName(),
                        expectedMessage, got.getClass().getSimpleName(), got.getMessage()));
            }
        };

        static interface Strategy {
            boolean matchesExpected(Class<? extends Throwable> expectedClass, Throwable got, String expectedMessage);

            void failWithExpectedButGot(Class<? extends Throwable> expectedClass, Throwable got, String expectedMessage);
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public static <T extends Throwable> void thrown(ExceptionMatch.Strategy matchStrategy,
                                                    Class<T> expectedThrowableClass,
                                                    Runnable block) {
        intercept(matchStrategy, expectedThrowableClass, block);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public static <T extends Throwable> void thrownWithMessage(ExceptionMatch.Strategy matchStrategy,
                                                               Class<T> expectedThrowableClass, String expectedMessage,
                                                               Runnable block) {
        intercept(matchStrategy, expectedThrowableClass, expectedMessage, block);
    }

    public static <T extends Throwable> void thrown(Class<T> expectedThrowableClass,
                                                    Runnable block) {
        thrown(EXCEPTION_CLASS_MUST_EQUAL, expectedThrowableClass, block);
    }

    public static <T extends Throwable> T intercept(Class<T> expectedThrowableClass,
                                                    Runnable block) {
        return intercept(EXCEPTION_CLASS_MUST_EQUAL, expectedThrowableClass, block);
    }

    public static <T extends Throwable> T intercept(ExceptionMatch.Strategy matchStrategy,
                                                    Class<T> expectedThrowableClass,
                                                    Runnable block) {
        return intercept(matchStrategy, expectedThrowableClass, null, block);
    }

    public static <T extends Throwable> T intercept(ExceptionMatch.Strategy matchStrategy,
                                                    Class<T> expectedThrowableClass, String expectedMessage,
                                                    Runnable block) {
        try {
            block.run();

            failWithExpectedButGotNothing(expectedThrowableClass); // will throw
            return null; // make compiler happy

        } catch (Throwable thr) {
            Class<? extends Throwable> gotThrowableClass = thr.getClass();

            boolean gotExpectedException = matchStrategy.matchesExpected(expectedThrowableClass, thr, expectedMessage);
            if (gotExpectedException) {
                return expectedThrowableClass.cast(thr);
            } else {
                matchStrategy.failWithExpectedButGot(expectedThrowableClass, thr, expectedMessage);
                return null; // make compiler happy
            }
        }
    }

    private static void failWithExpectedButGotNothing(Class<?> expected) {
        Assert.fail(String.format("Expected [%s] to be thrown but no exception was thrown.", expected.getSimpleName()));
    }

}
