/*******************************************************************************
 * Copyright (c) 2008 Heiko Seeberger and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Heiko Seeberger - initial implementation
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.service.weaving.ICachingService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;

/**
 * {@link ICachingService} used as "singleton" OSGi service by
 * "org.aspectj.osgi".
 * 
 * @author Heiko Seeberger
 */
public class SingletonCachingService extends BaseCachingService {

    private final Map<String, BundleCachingService> bundleCachingServices = new HashMap<String, BundleCachingService>();

    private final BundleContext bundleContext;

    /**
     * @param bundleContext
     *            Must not be null!
     * @throws IllegalArgumentException
     *             if given bundleContext is null.
     */
    public SingletonCachingService(final BundleContext bundleContext) {
        if (bundleContext == null) {
            throw new IllegalArgumentException(
                    "Argument \"bundleContext\" must not be null!");
        }
        this.bundleContext = bundleContext;

        this.bundleContext.addBundleListener(new SynchronousBundleListener() {

            public void bundleChanged(final BundleEvent event) {
                if (event.getType() == BundleEvent.UNINSTALLED) {
                    stopBundleCachingService(event);
                }
            }
        });
    }

    /**
     * Generates the unique id for the cache of the given bundle (usually the
     * symbolic name and the bundle version)
     * 
     * @param bundle
     *            The bundle for which a unique cache id should be calculated
     * @return The unique id of the cache for the given bundle
     */
    public String getCacheId(final Bundle bundle) {
        String bundleVersion = (String) bundle.getHeaders().get(
                Constants.BUNDLE_VERSION);
        if (bundleVersion == null || bundleVersion.length() == 0) {
            bundleVersion = "0.0.0"; //$NON-NLS-1$
        }
        return bundle.getSymbolicName() + "_" + bundleVersion; //$NON-NLS-1$
    }

    /**
     * Looks for a stored {@link BaseCachingService} for the given bundle. If
     * this exits it is returned, else created and stored.
     * 
     * @see org.eclipse.equinox.service.weaving.ICachingService#getInstance(java.lang.ClassLoader,
     *      org.osgi.framework.Bundle, java.lang.String)
     */
    @Override
    public synchronized ICachingService getInstance(
            final ClassLoader classLoader, final Bundle bundle, String key) {

        if (bundle == null) {
            throw new IllegalArgumentException(
                    "Argument \"bundle\" must not be null!");
        }

        final String cacheId = getCacheId(bundle);

        BundleCachingService bundleCachingService = bundleCachingServices
                .get(cacheId);

        key = key == null || key.length() == 0 ? "defaultCache" : key;

        if (bundleCachingService == null) {

            bundleCachingService = new BundleCachingService(bundleContext,
                    bundle, key);
            bundleCachingServices.put(cacheId, bundleCachingService);

            if (Log.isDebugEnabled()) {
                Log.debug(MessageFormat.format(
                        "Created BundleCachingService for [{0}].", cacheId));
            }
        }

        return bundleCachingService;
    }

    /**
     * Stops all individual bundle services.
     */
    public synchronized void stop() {
        for (final BundleCachingService bundleCachingService : bundleCachingServices
                .values()) {
            bundleCachingService.stop();
        }
        bundleCachingServices.clear();
    }

    /**
     * Stops individual bundle caching service if the bundle is uninstalled
     */
    protected void stopBundleCachingService(final BundleEvent event) {
        final String cacheId = getCacheId(event.getBundle());
        final BundleCachingService bundleCachingService = bundleCachingServices
                .get(cacheId);
        if (bundleCachingService != null) {
            bundleCachingService.stop();
            bundleCachingServices.remove(cacheId);
        }
    }

}
