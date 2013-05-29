/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Universität Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uka.ipd.idaho.gamta.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This class loader loads classes from jar files, which can be added in a
 * variaty of ways, in particular, as Files, URLs, and arbitrary InputStreams.
 * The GamtaClassLoader is intended to take the place of a URLClassLoader where
 * the surrounding code needs to access jar files that cannot be provided as
 * URLs. This ClassLoader caches all added jars in memory in the form of byte
 * arrays, indexed by the class names the jars provide. Once a class has been
 * loaded, the latter is cached as well. This prevents classes from being loaded
 * more than once, preventing problems with static members.
 * 
 * @author sautter
 */
public class GamtaClassLoader extends ClassLoader {
	
	/**
	 * A component initializer can be used to do initialization of components
	 * right after loading, with the information which jar file a given
	 * component was loaded from still available. This is in particular for
	 * setting data providers or data paths, which may depend on jar file names.
	 * <br>
	 * <br>
	 * Example: Suppose the component folder is 'D:/MyApplication/Components',
	 * and the component MyComponent resides in MyComponentJar.jar, then this
	 * component can have 'D:/MyApplication/Components/MyComponentJarData' as
	 * its data path (created automatically if not existing).
	 * 
	 * @author sautter
	 */
	public static interface ComponentInitializer {

		/**
		 * Initialize a component. The runtime type of the component objects
		 * handed to this method will be sub classes of the component class
		 * specified to the class loading method.
		 * @param component the component to initialize
		 * @param componentJarName the name of the jar file the component was
		 *            loaded from (file name only, since the path of the jar
		 *            file is the folder handed to the loading method)
		 * @throws Exception
		 */
		public abstract void initialize(Object component, String componentJarName) throws Throwable;
	}

	/**
	 * Load and instantiate components from the jar files residing in some
	 * folder. The runtime type of the component objects returned will be the
	 * specified component class or a sub class of it. If a component requires
	 * extra jars that are not on the surrounding application's class path,
	 * deposit them in '&lt;componentFolder&gt;/&lt;componentJarName&gt;Bin',
	 * where &lt;componentFolder&gt; is the specified folder and
	 * &lt;componentJarName&gt; is the name of the jar file the component was
	 * loaded from, without the '.jar' file extension.<br>
	 * <br>
	 * Example: Suppose the component folder is 'D:/MyApplication/Components',
	 * and the component MyComponent resides in MyComponentJar.jar, then all
	 * jars in 'D:/MyApplication/Components/MyComponentJarBin' will be placed on
	 * the class path for loading MyComponent. You have to create this folder
	 * manually in case you need to make some additional jar files available.
	 * @param componentFolder the folder containing the jar files to serch for
	 *            components
	 * @param componentSuperClass the common super class of all the components
	 *            to load
	 * @param componentInitializer an initializer that can do component class
	 *            specific initialization after a component is instanciated (may
	 *            be null)
	 * @return an array holding the components found in the jar files in the
	 *         specified folder
	 */
	public static Object[] loadComponents(final File componentFolder, Class componentSuperClass, ComponentInitializer componentInitializer) {
		
		// get base directory
		if (!componentFolder.exists()) componentFolder.mkdir();
		
		// get data names
		String[] dataNames = readFileList(componentFolder, (componentFolder.getAbsolutePath().length() + 1), 1);
		
		// load components
		return loadComponents(
				dataNames, 
				null, 
				new InputStreamProvider() {
					public InputStream getInputStream(String dataName) throws IOException {
						return new FileInputStream(new File(componentFolder, dataName));
					}
				}, 
				componentSuperClass, 
				componentInitializer);
	}

	private static String[] readFileList(File directory, int basePathLength, int depth) {
		if (depth < 0) return new String[0];
		StringVector resultFiles = new StringVector();
		File[] files = directory.listFiles();
		for (int f = 0; f < files.length; f++) {
			if (files[f].isDirectory() && !files[f].equals(directory)) resultFiles.addContent(readFileList(files[f], basePathLength, depth - 1));
			else resultFiles.addElement(files[f].getAbsolutePath().substring(basePathLength).replaceAll("\\\\", "\\/"));
		}
		return resultFiles.toStringArray();
	}

	/**
	 * An InputStreamProvider resolves data names and provides InputStreams to
	 * read them, similar to the getInputStream() method of
	 * AnalyzerDataProvider. This extra interface exists so components that do
	 * not have an AnalyzerDataProvider available can use the loadComponents()
	 * method.
	 * 
	 * @author sautter
	 */
	public static interface InputStreamProvider {
		
		/**
		 * Obtain an InputStream for an arbitrary data object. It is the
		 * invoking component's responsibility to close the stream after done
		 * with it.
		 * @param dataName the name of the data object
		 * @return an InputStream pointing to the data object with the specified
		 *         name, or null, if there is no such data object
		 * @throws IOException if any occurs while creating the InputStream
		 */
		public abstract InputStream getInputStream(String dataName) throws IOException;
	}

	/**
	 * Load and instantiate components from the jar files provided by an
	 * AnalyzerDataProvider. The runtime type of the component objects returned
	 * will be the specified component class or a sub class of it. If a
	 * component requires extra jars that are not on the surrounding
	 * application's class path, deposit them in a '&lt;componentJarName&gt;Bin'
	 * sub path, where &lt;componentJarName&gt; is the name of the jar file the
	 * component was loaded from, without the '.jar' file extension.<br>
	 * <br>
	 * Example: Suppose the component MyComponent resides in MyComponentJar.jar,
	 * then all jars in the path 'MyComponentJarBin/' will be placed on the
	 * class path for loading MyComponent.
	 * @param dataProvider the data provider providing the jars to load
	 * @param dataNamePrefix a prefix for filtering the data items
	 * @param componentSuperClass the common super class of all the components
	 *            to load
	 * @param componentInitializer an initializer that can do component class
	 *            specific initialization after a component is instanciated (may
	 *            be null)
	 * @return an array holding the components found in the jar files available
	 *         from the specified data provider
	 */
	public static Object[] loadComponents(final AnalyzerDataProvider dataProvider, String dataNamePrefix, Class componentSuperClass, ComponentInitializer componentInitializer) {
		return loadComponents(
				dataProvider.getDataNames(), 
				dataNamePrefix, 
				new InputStreamProvider() {
					public InputStream getInputStream(String dataName) throws IOException {
						return dataProvider.getInputStream(dataName);
					}
				}, 
				componentSuperClass, 
				componentInitializer);
	}
	
	/**
	 * Load and instantiate components from the jar files contained in a list of
	 * data items. The runtime type of the component objects returned will be
	 * the specified component class or a sub class of it. If a component
	 * requires extra jars that are not on the surrounding application's class
	 * path, deposit them in
	 * '&lt;componentFolder&gt;/&lt;componentJarName&gt;Bin', where
	 * &lt;componentFolder&gt; is the specified folder and
	 * &lt;componentJarName&gt; is the name of the jar file the component was
	 * loaded from, without the '.jar' file extension.<br>
	 * <br>
	 * Example: Suppose the component MyComponent resides in MyComponentJar.jar,
	 * then all jars in the path 'MyComponentJarBin/' will be placed on the
	 * class path for loading MyComponent.
	 * @param dataNames an array holding the names of the data items to search
	 *            for components
	 * @param dataNamePrefix a prefix for filtering the data items
	 * @param isProvider a resolver that resolves the names of the specified
	 *            data items to InputStreams
	 * @param componentSuperClass the common super class of all the components
	 *            to load
	 * @param componentInitializer an initializer that can do component class
	 *            specific initialization after a component is instanciated (may
	 *            be null)
	 * @return an array holding the components found in the jar files found in
	 *         the specified list
	 */
	public static Object[] loadComponents(String[] dataNames, String dataNamePrefix, InputStreamProvider isProvider, Class componentSuperClass, ComponentInitializer componentInitializer) {

		// normalize prefix
		if (dataNamePrefix == null) dataNamePrefix = "";
		else if ((dataNamePrefix.length() != 0) && !dataNamePrefix.endsWith("/")) dataNamePrefix += "/";

		System.out.println("GamtaClassLoader: loading components from data list:");
		System.out.println("  component class: " + componentSuperClass.getName());
		System.out.println("  path prefix: " + dataNamePrefix);
		System.out.println("  data list names are:");
//		for (int d = 0; d < dataNames.length; d++)
//			System.out.println("  - " + dataNames[d]);
		
		// get jar names
		String[] jarNames = getJarNames(dataNames, dataNamePrefix);
		Arrays.sort(jarNames);
		
		// examine jars
		StringVector jarNameList = new StringVector();
		StringVector jarClassNames = new StringVector();
		Properties jarNamesByClassNames = new Properties();
		System.out.println("  investigating jars:");
		for (int j = 0; j < jarNames.length; j++) {
			String jarName = jarNames[j];
			System.out.println("  - " + jarName);
			try {
				JarInputStream jis = new JarInputStream(isProvider.getInputStream(jarName));
				JarEntry je;
				while ((je = jis.getNextJarEntry()) != null) {
					String jarEntryName = je.getName();

					// new class file
					if (jarEntryName.endsWith(".class")) {
						String className = StringUtils.replaceAll(jarEntryName.substring(0, (jarEntryName.length() - 6)), "/", ".");

						// collect names of all non-nested classes
						if (className.indexOf('$') == -1) {
							jarClassNames.addElementIgnoreDuplicates(className);
							jarNamesByClassNames.setProperty(className, jarName);
							System.out.println("    - " + className);
						}
					}
				}
				jis.close();

				// add name of jar to list of jars to load
				jarNameList.addElement(jarName);

				// check for binary folder
				String[] jarBinJarNames = getJarNames(dataNames, (jarName.substring(0, (jarName.length() - 4)) + "Bin"));
				for (int jbj = 0; jbj < jarBinJarNames.length; jbj++) {
					System.out.println("    - " + jarBinJarNames[jbj]);
					jarNameList.addElement(jarBinJarNames[jbj]);
				}
			}
			catch (IOException ioe) {
				System.out.println("  could not access jar file '" + jarName + "': " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
		}
		
		// check for shared binary folder
		System.out.println("  adding shared jars:");
		String[] binJarNames = getJarNames(dataNames, (dataNamePrefix + "Bin"));
		for (int bj = 0; bj < binJarNames.length; bj++) {
			System.out.println("    - " + binJarNames[bj]);
			jarNameList.addElement(binJarNames[bj]);
		}
		
		// create class loader
		GamtaClassLoader componentLoader = createClassLoader(componentSuperClass);
		for (int j = 0; j < jarNameList.size(); j++) try {
			InputStream jis = isProvider.getInputStream(jarNameList.get(j));
			componentLoader.addJar(jis);
			jis.close();
		}
		catch (IOException ioe) {
			System.out.println("  could not access jar '" + jarNameList.get(j) + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
		
		// iterate over jar entries
		ArrayList componentList = new ArrayList();
		for (int jcn = 0; jcn < jarClassNames.size(); jcn++) {
			
			String className = jarClassNames.get(jcn);
			System.out.println("GamtaClassLoader: investigating class - " + className);
			Class componentClass = null;
			
			// try to load class
			try {
				componentClass = componentLoader.loadClass(className);
			}
			catch (ClassNotFoundException cnfe) {
				System.out.println("  class not found.");
			}
			catch (NoClassDefFoundError ncdfe) {
				System.out.println("  required class not found: " + ncdfe.getMessage() + ".");
			}
			catch (LinkageError le) {
				System.out.println("  class linkage failed: " + le.getMessage() + " (" + le.getClass().getName() + ").");
			}
			catch (SecurityException se) { // may happen due to jar signatures
				System.out.println("  not allowed to load class.");
			}
			
			// class loaded successfully
			if (true
					&& (componentClass != null)
					&& !Modifier.isAbstract(componentClass.getModifiers())
					&& Modifier.isPublic(componentClass.getModifiers())
					&& !Modifier.isInterface(componentClass.getModifiers())
					&& componentSuperClass.isAssignableFrom(componentClass)
				) {
				System.out.println("  got component class");
				
				try {
					Object component = componentClass.newInstance();
					System.out.println("  component class successfully instantiated.");
					
					String jarName = jarNamesByClassNames.getProperty(className);
					if (componentInitializer != null)
						componentInitializer.initialize(component, jarName);
					componentList.add(component);
				}
				catch (InstantiationException e) {
					System.out.println("  could not instantiate component class.");
				}
				catch (IllegalAccessException e) {
					System.out.println("  illegal access to component class.");
				}
				catch (NoClassDefFoundError ncdfe) {
					System.out.println("  could not find some part of component class: " + ncdfe.getMessage());
				}
				catch (AccessControlException ace) {
					Permission p = ace.getPermission();
					if (p == null) System.out.println("  plugin violated security constraint.");
					else System.out.println("  plugin violated security constraint, permission '" + p.getActions() + "' was denied for '" + p.getName() + "' by runtime environment.");
				}
				catch (Exception e) {
					System.out.println("  could not initialize component class: " + e.getMessage());
				}
				catch (Throwable t) {
					System.out.println("  could not initialize component class: " + t.getMessage());
					t.printStackTrace(System.out);
				}
			}
		}

		return componentList.toArray();
	}
	
	private static String[] getJarNames(String[] dataNames, String prefix) {
		if ((prefix.length() != 0) && !prefix.endsWith("/")) prefix += "/";
		StringVector dataNameList = new StringVector();
		for (int f = 0; f < dataNames.length; f++) {
			if (dataNames[f].startsWith(prefix) && dataNames[f].endsWith(".jar") && (dataNames[f].substring(prefix.length()).indexOf('/') == -1))
				dataNameList.addElementIgnoreDuplicates(dataNames[f]);
		}
		return dataNameList.toStringArray();
	}

	private static final boolean DEBUG = false;
	
	
	/**
	 * Create a new GAMTA class loader. If the argument class object is not
	 * null, the parent class loader for the threaded class loader will be
	 * retrieved from this class.
	 * @param parentClassLoaderOwner the owner object of the parent class loader to
	 *            use
	 * @return a new GAMTA class loader
	 */
	public static GamtaClassLoader createClassLoader(Class parentClassLoaderOwner) {
		if (isThreadLocal()) {
			ClassLoaderRequest clr = new ClassLoaderRequest(parentClassLoaderOwner);
			return clr.getClassLoader();
		}
		else {
			if (parentClassLoaderOwner == null)
				return new GamtaClassLoader();
			else return new GamtaClassLoader(parentClassLoaderOwner.getClassLoader());
		}
	}
	
	private static final boolean DEBUG_FACTORY = false;
	private static LinkedList classLoaderRequestQueue = new LinkedList();
	private static ClassLoaderFactory classLoaderFactory = null;
	
	private static class ClassLoaderFactory extends Thread {
		private boolean keepRunning = true;
		public void run() {
			while (this.keepRunning) {
				ClassLoaderRequest clr = null;
				synchronized(classLoaderRequestQueue) {
					if (classLoaderRequestQueue.isEmpty()) try {
						classLoaderRequestQueue.wait();
						if (DEBUG_FACTORY) System.out.println("  factory woken up");
					} catch (InterruptedException ie) {}
					else clr = ((ClassLoaderRequest) classLoaderRequestQueue.removeFirst());
				}
				if (clr != null) {
					if (DEBUG_FACTORY) System.out.println("  got class loader request");
					
					try {
						GamtaClassLoader cl;
						if (clr.parentClassLoaderOwner == null)
							cl = new GamtaClassLoader();
						else cl = new GamtaClassLoader(clr.parentClassLoaderOwner.getClassLoader());
						if (DEBUG_FACTORY) System.out.println("  got class loader");
						clr.setConnection(cl, null);
					}
					catch (SecurityException se) {
						se.printStackTrace(System.out);
						clr.setConnection(null, se);
					}
					
					if (DEBUG_FACTORY) System.out.println("  class loader passed to request");
				}
			}
		}
		
		void shutdown() {
			this.keepRunning = false;
			synchronized(classLoaderRequestQueue) {
				classLoaderRequestQueue.notify();
			}
		}
	}
	
	private static class ClassLoaderRequest {
		Class parentClassLoaderOwner;
		private Object lock = new Object();
		private GamtaClassLoader cl;
		private SecurityException se;
		ClassLoaderRequest(Class parentClassLoaderOwner) {
			this.parentClassLoaderOwner = parentClassLoaderOwner;
		}
		GamtaClassLoader getClassLoader() throws SecurityException {
			synchronized(classLoaderRequestQueue) {
				classLoaderRequestQueue.addLast(this);
				if (DEBUG_FACTORY) System.out.println("  request enqueued");
				classLoaderRequestQueue.notify();
				if (DEBUG_FACTORY) System.out.println("  factory notified");
			}
			synchronized(this.lock) {
				if ((this.cl == null) && (this.se == null)) {
					try {
						if (DEBUG_FACTORY) System.out.println("  waiting");
						this.lock.wait();
					} catch (InterruptedException ie) {}
				}
				else if (DEBUG_FACTORY) System.out.println("  good service is fast :-)");
			}
			if (DEBUG_FACTORY) System.out.println("  requester woken up");
			if (this.se == null) {
				if (DEBUG_FACTORY) System.out.println("  returning class loader");
				return this.cl;
			}
			else {
				if (DEBUG_FACTORY) System.out.println("  throwing exception " + this.se.getMessage());
				throw this.se;
			}
		}
		void setConnection(GamtaClassLoader cl, SecurityException se) {
			synchronized(this.lock) {
				this.cl = cl;
				this.se = se;
				if (DEBUG_FACTORY) System.out.println("  class loader stored in request");
				this.lock.notify();
			}
		}
	}
	
	/**
	 * Check if GamtaClassLoader uses a dedicated thread for creating instances,
	 * and if these instances use dedicated threads for loading classes.
	 * @return the thread local.
	 */
	public static boolean isThreadLocal() {
		return (classLoaderFactory != null);
	}
	
	/**
	 * Specify whether or not GamtaClassLoader instances should be created and
	 * should load classes in a dedicated thread. In the presence of an eager
	 * SecurityManager, for instance, in an applet, this is necessary for
	 * offering class loading functionality to instances of classes that where
	 * loaded through subordinate class loaders instead of the system class
	 * loader itself. Even if such plugin instances originate from certified
	 * code, the SecurityManager won't allow them to create class loaders or
	 * load classes. Setting the threadLocal property will cause
	 * GamtaClassLoader instances to be produced and to work in a dedicated
	 * service thread, thus in code loaded entirely through the system class
	 * loader, and then hand out the loaded classes to the requesting threads.
	 * This circumvents the security restrictions. This property should be set
	 * as early as possible.
	 * @param threadLocal use a dedicated thread for loading classes?
	 */
	public static void setThreadLocal(boolean threadLocal) {
		if (threadLocal) {
			
			//	don't start twice
			if (classLoaderFactory == null) {
				classLoaderFactory = new ClassLoaderFactory();
				classLoaderFactory.start();
			}
		}
		else {
			
			//	check if something to shut down
			if (classLoaderFactory != null) {
				classLoaderFactory.shutdown();
				classLoaderFactory = null;
			}
		}
	}
	
	
	
	
	private TreeMap classBytesByName = new TreeMap();
	private TreeMap classesByName = new TreeMap();
//	private ClassFactory classFactory = null;
	
	/**
	 * Constructor
	 */
	private GamtaClassLoader() {
		super();
	}
	
	/**
	 * Constructor
	 * @param parent the parent ClassLoader
	 */
	private GamtaClassLoader(ClassLoader parent) {
		super(parent);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#getResourceAsStream(java.lang.String)
	 */
	public InputStream getResourceAsStream(String name) {
		byte[] bytes = ((byte[]) this.classBytesByName.get(name));
		return ((bytes == null) ? super.getResourceAsStream(name) : new ByteArrayInputStream(bytes));
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.ClassLoader#findClass(java.lang.String)
	 */
	protected Class findClass(String name) throws ClassNotFoundException {
		Class foundClass = ((Class) this.classesByName.get(name));
		
		if (foundClass == null) {
			if (DEBUG) System.out.println("GCL: Loading class '" + name + "' ...");
			
			String classDataName = name.replaceAll("\\.", "/");
			
			if (!classDataName.endsWith(".class")) classDataName += ".class";
			if (DEBUG) System.out.println("GCL:  resource name is '" + classDataName + "'");
			
			byte[] classBytes = ((byte[]) this.classBytesByName.get(classDataName));
			if (classBytes != null) {
				if (DEBUG) System.out.println("GCL:  got '" + classBytes.length + "' bytes of byte code");
				foundClass = this.defineClass(name, classBytes, 0, classBytes.length);
				if (DEBUG) System.out.println("GCL:  class defined");
				this.resolveClass(foundClass);
				if (DEBUG) System.out.println("GCL:  class resolved");
				this.classesByName.put(name, foundClass);
				if (DEBUG) System.out.println("GCL:  class loaded & cached");
			}
		}
		
		if (foundClass == null) {
			if (DEBUG) System.out.println("GCL:  class '" + name + "' not found");
			throw new ClassNotFoundException(name);
		}
		else return foundClass;
	}
	
	/**
	 * Add a jar file in the form of a file object
	 * @param jarFile the jar file to add
	 * @throws IOException
	 */
	public void addJar(File jarFile) throws IOException {
		InputStream jis = new FileInputStream(jarFile);
		this.addJar(jis);
		jis.close();
	}
	
	/**
	 * Add a jar file in the form of a URL
	 * @param jarUrl the URL to load the jar from
	 * @throws IOException
	 */
	public void addJar(URL jarUrl) throws IOException {
		InputStream jis = jarUrl.openStream();
		this.addJar(jis);
		jis.close();
	}

	/**
	 * Add a jar file, loading it from an arbitrary InputStream
	 * @param jarSource the InputStream providing access to the jar to add
	 * @throws IOException
	 */
	public void addJar(InputStream jarSource) throws IOException {
		if (DEBUG) System.out.println("Adding jar ...");
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int read;
			while ((read = jarSource.read(buf)) != -1)
				baos.write(buf, 0, read);

			byte[] jarBytes = baos.toByteArray();
			if (DEBUG) System.out.println("  loaded " + jarBytes.length + " bytes of jar.");

			ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(jarBytes));
			ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null) {
//				this.jarsByEntryNames.put(ze.getName(), jarBytes);
//				if (DEBUG) System.out.println("  indexed jar for class " + ze.getName());
				
				baos = new ByteArrayOutputStream();
				while ((read = zis.read(buf)) != -1)
					baos.write(buf, 0, read);
				
				this.classBytesByName.put(ze.getName(), baos.toByteArray());
//				if (DEBUG) System.out.println("  indexed bytes of jar entry " + ze.getName());
			}
		}
		catch (IOException ioe) {
			System.out.println("Exception loading jar: " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			throw ioe;
		}
	}
}
