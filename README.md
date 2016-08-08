# CDI extensions

This is a WELD 2.0 port of the SoftwareMill Common CDI auto factories.
Original repository: https://github.com/softwaremill/softwaremill-common
*Work in progress*

## Autofactories, or assisted inject implementation for CDI

Assisted inject originates from Guice: http://code.google.com/p/google-guice/wiki/AssistedInject

Using autofactories can save you some time writing simple factories which inject beans and pass them in the
constructor to create the target bean.

For example if you have:

    interface PriceCalculator {
        int getPriceAfterDiscounts();

        interface Factory {
            PriceCalculator create(Product product);
        }
    }

and an implementation (`PriceCalculatorImpl`) which calculates the discount basing on an instance of a Discounts bean,
instead of writing an implementation of the Factory yourself, you can just do:

    @CreatedWith(PriceCalculator.Factory.class)
    public class PriceCalculatorImpl implements PriceCalculator {
        private final Discounts discounts;
        private final Product product;

        @Inject
        public PriceCalculatorImpl(Discounts discounts, @FactoryParameter Product product) {
            this.discounts = discounts;
            this.product = product;
        }

        int getPriceAfterDiscounts() {
            return product.getPrice() - discounts.getNormalDiscount();
        }
    }

Note the usage of the annotations:
- `@CreatedWith` specifies the factory interface, for which an implementation will be created. The interface
should have only one method (later referred to as the factory method)
- `@FactoryParameter` specifies that the annotated constructor parameter corresponds to a parameter of the same class
in the factory method
- `@Inject` specifies that other parameters should be injected from the context

You can then use the factory as any other bean:

    public class Test {
        @Inject
        private PriceCalculator.Factory priceCalculatorFactory;

        public void test() {
            assertThat(priceCalculatorFactory.create(new Product(100)).getPriceAfterDiscounts()).isEqualTo(90);
        }
    }

Alternatively, if you don't want to mix dependencies and factory parameters in the constructor, you can use field
or setter injection:

    @CreatedWith(PriceCalculator.Factory.class)
    public class PriceCalculatorImpl implements PriceCalculator {
        @Inject
        private Discounts discounts;

        private final Product product;

        public PriceCalculatorImpl(Product product) {
            this.product = product;
        }

        int getPriceAfterDiscounts() {
            return product.getPrice() - discounts.getNormalDiscount();
        }
    }

The `@Inject` annotation on the constructor and the `@FactoryParameter` annotations aren't needed in this case.
