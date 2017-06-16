/*******************************************************************************
 * Copyright (c) 1998, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.device;

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Calendar;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * LogTracker class. This class encapsulates the LogService
 * and handles all issues such as the service coming and going.
 */

public class LogTracker extends ServiceTracker {
	/** LogService interface class name */
	protected final static String clazz = "org.osgi.service.log.LogService"; //$NON-NLS-1$

	/** PrintStream to use if LogService is unavailable */
	protected PrintStream out;

	/** Calendar and DateFormat to user if LogService is unavailable */
	private static Calendar calendar;
	private static DateFormat dateFormat;
	private String timestamp;

	/**
	 * Create new LogTracker.
	 *
	 * @param context BundleContext of parent bundle.
	 * @param out Default PrintStream to use if LogService is unavailable.
	 */
	public LogTracker(BundleContext context, PrintStream out) {
		super(context, clazz, null);
		this.out = out;
		calendar = Calendar.getInstance();
		dateFormat = DateFormat.getDateTimeInstance();
		open();
	}

	/*
	 * ----------------------------------------------------------------------
	 *      LogService Interface implementation
	 * ----------------------------------------------------------------------
	 */

	public void log(int level, String message) {
		log(null, level, message, null);
	}

	public void log(int level, String message, Throwable exception) {
		log(null, level, message, exception);
	}

	public void log(ServiceReference reference, int level, String message) {
		log(reference, level, message, null);
	}

	public synchronized void log(ServiceReference reference, int level, String message, Throwable exception) {
		ServiceReference[] references = getServiceReferences();

		if (references != null) {
			int size = references.length;

			for (int i = 0; i < size; i++) {
				LogService service = (LogService) getService(references[i]);
				if (service != null) {
					try {
						service.log(reference, level, message, exception);
					} catch (Exception e) {
						// do nothing
					}
				}
			}

			return;
		}

		noLogService(level, message, exception, reference);
	}

	/**
	 * The LogService is not available so we write the message to a PrintStream.
	 *
	 * @param level Logging level
	 * @param message Log message.
	 * @param throwable Log exception or null if none.
	 * @param reference ServiceReference associated with message or null if none.
	 */
	protected void noLogService(int level, String message, Throwable throwable, ServiceReference reference) {
		if (out != null) {
			synchronized (out) {
				// Bug #113286.  If no log service present and messages are being
				// printed to stdout, prepend message with a timestamp.
				timestamp = dateFormat.format(calendar.getTime());
				out.print(timestamp + " "); //$NON-NLS-1$

				switch (level) {
					case LogService.LOG_DEBUG : {
						out.print("Debug: "); //$NON-NLS-1$

						break;
					}
					case LogService.LOG_INFO : {
						out.print(LogTrackerMsg.Info);

						break;
					}
					case LogService.LOG_WARNING : {
						out.print(LogTrackerMsg.Warning);

						break;
					}
					case LogService.LOG_ERROR : {
						out.print(LogTrackerMsg.Error);

						break;
					}
					default : {
						out.print("["); //$NON-NLS-1$
						out.print(LogTrackerMsg.Unknown_Log_level);
						out.print("]: "); //$NON-NLS-1$

						break;
					}
				}

				out.println(message);

				if (reference != null) {
					out.println(reference);
				}

				if (throwable != null) {
					throwable.printStackTrace(out);
				}
			}
		}
	}
}
