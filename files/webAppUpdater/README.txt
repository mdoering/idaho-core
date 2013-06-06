Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Universität Karlsruhe (TH) nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


WebAppUpdater is a tool for servlet based web applications that require post-
deployment configuration, e.g. to access a database. Such web applications
cannot be distributed in WAR files because the WAR deployment mechanism deletes
any prior version before un-packing a more recent one, destroying all
configuration in the process. This tool deploys and updates web applications
from zipped 'exploded archive directories'. It seeks the ZIP files first in the
root folder of the web application itself, then in the webapps folder of the
servlet container, i.e., one level up the directory tree.


The JAR takes the name of the web application as an argument; if it is started
inside a web application folder and the argument is missing, it uses the folder
name. The default scripts loop through their command line arguments. Projects
are welcome to deliver Updater.jar together with custom scripts that include the
web application name.


To exclude files from being updated, file names (including the path inside the
web application root folder) and patterns (use '*' as the wildcard) can be
stored in a file names 'update.cnfg' directly at in the web application root
folder.


If a web application is initially deployed from a ZIP file using the update tool
in the 'webapps' folder of the servlet container, it copies itself and generated
scripts into the web application root folder for convenience. This is unless
the ZIP contains respective files itself.


Usage in 'webapps' folder:
  update <webAppName> <zipName>?
This deploys or updates the web application named <webAppName> form a file named
<zipName>, which it first seeks inside the folder <webAppName>, then in the
'webapps' folder itself. Omitting the <zipName> argument is equivalent to
  update <webAppName> <webAppName>.zip

Usage in root folder of web application <webApp>:
  update <zipName>?
This updates the web application named <webApp> form a file named <zipName>,
which it again first seeks inside the folder <webApp>, then in the 'webapps'
folder itself. Omitting the <zipName> argument is equivalent to
  update <webApp>.zip