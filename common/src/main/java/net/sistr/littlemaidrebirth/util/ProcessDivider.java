package net.sistr.littlemaidrebirth.util;

import java.util.Optional;

public interface ProcessDivider<T> {

    default boolean tick(int count) {
        for (int i = 0; i < count; i++) {
            if (isEnd()) {
                return hasResult();
            }
            if (tick()) {
                return true;
            }
        }
        return false;
    }

    boolean tick();

    default boolean hasResult() {
        return getResult().isPresent();
    }

    Optional<T> getResult();

    boolean isEnd();

}
