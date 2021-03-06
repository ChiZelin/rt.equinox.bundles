/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestWBServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final String STATUS_PARAM = "status";
	
	private final AtomicReference<String> status = new AtomicReference<String>("none");
	private final AtomicBoolean destroyed = new AtomicBoolean(false);

	@Override
	public void init() throws ServletException {
		status.set(getServletConfig().getInitParameter(STATUS_PARAM));
	}

	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter writer = response.getWriter();
		try {
			handleDoGet(request, writer);
		} finally {
			writer.close();
		}
	}

	protected void handleDoGet(HttpServletRequest request, PrintWriter writer) throws ServletException, IOException {
		if (destroyed.get()) {
			writer.print("destroyed");
		} else {
			writer.print(status.get());
		}
	}

	@Override
	public void destroy() {
		super.destroy();
	}

	public boolean isDestroyed() {
		return destroyed.get();
	}
}
