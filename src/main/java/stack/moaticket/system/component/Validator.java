package stack.moaticket.system.component;

import org.springframework.stereotype.Component;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
public class Validator {
    public <T> Chain<T> of(T value) { return new Chain<>(value); }

    public static final class Chain<T> {
        private T value;
        private boolean skipFlag;

        private Chain(T value) {
            this.value = value;
            this.skipFlag = false;
        }

        private boolean skip() { return skipFlag; }
        private void applySkip() { skipFlag = true; }

        public Chain<T> validateOrThrow(Predicate<T> predicate, MoaExceptionType type) {
            if(skip()) return this;
            if(predicate.test(value)) throw new MoaException(type);

            return this;
        }

        public Chain<T> validateIfOrThrow(Predicate<T> predicate, Consumer<T> onSuccess, MoaExceptionType type) {
            if(skip()) return this;
            if(predicate.test(value)) onSuccess.accept(value);
            else throw new MoaException(type);

            return this;
        }

        public Chain<T> validateOrElse(Predicate<T> predicate, Function<T, T> onFail) {
            if(skip()) return this;
            if(predicate.test(value)) value = onFail.apply(value);

            return this;
        }

        public Chain<T> validateIfOrElse(Predicate<T> predicate, Function<T, T> onSuccess, Consumer<T> onFail) {
            if(skip()) return this;
            if(predicate.test(value)) value = onSuccess.apply(value);
            else onFail.accept(value);

            return this;
        }

        public Chain<T> executeIf(Predicate<T> predicate, Consumer<T> onSuccess) {
            if(skip()) return this;
            if(predicate.test(value)) {
                onSuccess.accept(value);
                applySkip();
            }

            return this;
        }

        public Chain<T> executeIfOrElse(Predicate<T> predicate, Function<T, T> onSuccess, Consumer<T> onFail) {
            if(skip()) return this;
            if(predicate.test(value)) {
                value = onSuccess.apply(value);
                applySkip();
            }
            else {
                onFail.accept(value);
            }

            return this;
        }

        public T get() { return value; }
    }
}