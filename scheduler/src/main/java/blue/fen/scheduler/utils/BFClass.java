package blue.fen.scheduler.utils;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 */
public final class BFClass {
    public interface BFCleanable {
        void clean();
    }

    public interface BFDestroyable {
        void destroy();
    }

    public interface Factory<T> {
        T factory();
    }

    public static <T> Default.Impl<T> defaultImpl(Factory<T> factory) {
        return new Default.Impl<T>(factory);
    }

    public static abstract class Default<T> {
        public T factory(T t) {
            return t != null ? t : impl();
        }

        public abstract T impl();

        public static class Impl<T> extends Default<T> {
            Factory<T> factory;

            public Impl(Factory<T> factory) {
                this.factory = factory;
            }

            @Override
            public T impl() {
                return factory.factory();
            }
        }
    }

    public static <T> BFSingleton<T> singleton(Factory<T> factory) {
        return new Singleton<T>(factory);
    }

    private final static class Singleton<T> extends BFSingleton<T> {
        Factory<T> factory;

        public Singleton(Factory<T> factory) {
            this.factory = factory;
        }

        @Override
        protected T create() {
            return factory.factory();
        }
    }
}
