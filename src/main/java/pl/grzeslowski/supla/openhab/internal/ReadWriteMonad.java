/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * <p>See the NOTICE file(s) distributed with this work for additional information.
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 */
package pl.grzeslowski.supla.openhab.internal;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
public final class ReadWriteMonad<T> {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final T t;

    public ReadWriteMonad(@NonNull final T t) {
        this.t = requireNonNull(t);
    }

    public void doInWriteLock(Consumer<T> consumer) {
        lock.writeLock().lock();
        try {
            consumer.accept(t);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void doInReadLock(Consumer<T> consumer) {
        lock.readLock().lock();
        try {
            consumer.accept(t);
        } finally {
            lock.readLock().unlock();
        }
    }
}
