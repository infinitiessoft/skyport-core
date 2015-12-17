package com.infinities.skyport.quartz;

import java.io.Closeable;
import java.util.concurrent.ScheduledFuture;

public interface QuartzService<K extends Enum<K>> extends Closeable {

	<T> ScheduledFuture<?> schedule(K k, QuartzConfiguration<T> configuration);

	<T> ScheduledFuture<?> executeOnce(K k);

}
