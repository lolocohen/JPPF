<?php $currentPage="Features" ?>
$template{name="about-page-header" title="Features"}$

<h1>JPPF 3.3 features</h1>

<h3>Ease of use</h3>
<ul>
	<li>simple APIs requiring small or no learning curve</li>
	<li>automatic deployment of application code on the grid</li>
	<li>ability to reuse existing or legacy objects without modification</li>
	<li>"happy path" with no additional configuration</li>
	<li>automatic server discovery, combinable with manual connection configuration</li>
	<li>convenient reusable application template to quickly and easily start developing JPPF applications</li>
	<li>straightforward Executor Service interface to the JPPF grid, with high throughput enhancements</li>
</ul>

<h3>Security</h3>
<ul>
	<li>secure network communications via SSL / TLS</li>
	<li>data encryption</li>
	<li>data integrity</li>
	<li>certificate-based authentication, including 2 ways (server and client) authentication</li>
	<li>plain and secure communications can be used separately or together</li>
</ul>

<h3>Cloud ready</h3>
<ul>
	<li>JPPF is ideally suited for cloud deployment</li>
	<li>native dynamic topology makes cloud on-demand scaling transparent</li>
	<li>multiple extensible options for server discovery in the cloud</li>
	<li>documented deployment on Amazon EC2</li>
</ul>

<h3>Self-repair and recovery</h3>
<ul>
	<li>automated node reconnection with failover strategy</li>
	<li>automated client reconnection with failover strategy</li>
	<li>fault tolerance with job requeuing</li>
	<li>detection, recovery from hard-disconnects of remote nodes</li>
</ul>

<h3>Job-level SLA</h3>
<ul>
	<li>job execution policies enable rule-based node filtering</li>
	<li>maximum number of nodes a job can run on (grid partitioning)</li>
	<li>dynamic job prioritization</li>
	<li>job scheduled start date</li>
	<li>job scheduled expiration date</li>
	<li>broadcast jobs</li>
</ul>

<h3>Job local monitoring, persistence, recovery, failover</h3>
<ul>
	<li>notification of job start and stop events</li>
	<li>ability to store job snapshots on the fly</li>
	<li>recovery of the latest job state upon application crash</li>
</ul>

<h3>Management and monitoring</h3>
<ul>
	<li>task-level events</li>
	<li>job-level events</li>
	<li>server performance statistics</li>
	<li>server performance charts</li>
	<li>user-defined charts</li>
	<li>remote server control and monitoring</li>
	<li>remote nodes control and monitoring</li>
	<li>cpu utilization monitoring</li>
	<li>management of load-balancing</li>
	<li>management and monitoring available via APIs and graphical user interface (administration console)</li>
	<li>access to remote servers and nodes logs via the JMX-based logger (integrates with Log4j and JDK logging)</li>
</ul>

<h3>Platform extensibility</h3>
<ul>
	<li>All management beans are pluggable, users can add their own management modules at server or node level</li>
	<li>Startup classes: users can add their own initialization modules at server and node startup</li>
	<li>Security: any data transiting over the network can now be encrypted by the way of user-defined transformations</li>
	<li>Pluggable load-balancing modules allow users to write their own load balancing strategies</li>
	<li>Ability to specify alternative serialization schemes</li>
	<li>Subscription to nodes life cycle events</li>
	<li>Node initialization hooks to implement dynamic failover strategies</li>
	<li>Server-side notifications of node connection events</li>
</ul>

<h3>Dynamic class loading extensions</h3>
<ul>
	<li>update the classpath of the nodes at run-time</li>
	<li>download multiple classpath resources in a single network transaction</li>
	<li>remotely manage repositories of application libraries</li>
	<li>new class loader delegation model enables faster class loading</li>
</ul>

<h3>Performance and resources efficiency</h3>
<ul>
	<li>multiple configurable load-balancing algorithms</li>
	<li>adaptive load-balancing adjusts in real-time to workload changes</li>
	<li>memory-aware components with disk overflow</li>
	<li>client-side server connection pools</li>
</ul>

<h3>Dynamic topology scaling</h3>
<ul>
	<li>nodes can be added and removed dynamically from the grid</li>
	<li>servers can be added and removed dynamically from the grid</li>
	<li>servers can work alone or linked in P2P topology with other servers</li>
	<li>ability to run a node in the same JVM as the server</li>
</ul>

<h3>Third-party connectors</h3>
<ul>
	<li>J2EE connector, JCA 1.5 compliant, deployed as a standard resource adapter</li>
	<li>Apache Tomcat connector</li>
	<li>GigaSpaces XAP connector</li>
</ul>

<h3>Ease of integration</h3>
<ul>
	<li><a href="samples-pack/FTPServer/Readme.php">Apache FTP server</a></li>
	<li><a href="samples-pack/DataDependency/Readme.php">Hazelcast data grid</a></li>
	<li><a href="samples-pack/NodeLifeCycle/Readme.php">Atomikos transaction manager and JDBC database</a></li>
</ul>

<h3>Deployment modes</h3>
<ul>
	<li>all components deployable as standalone Java applications</li>
	<li>servers and nodes deployable as Linux/Unix daemons</li>
	<li>servers and nodes deployable as Windows services</li>
	<li>application client deployment as a Web, J2EE or GigaSpaces XAP application
	<li>nodes can run in idle system mode (CPU scavenging)</li>
	<li>deployment on Amazon EC2</li>
</ul>

<h3>Execution modes</h3>
<ul>
	<li>synchronous and asynchronous job submissions</li>
	<li>client can execute in local mode (benefits to systems with many CPUs)</li>
	<li>client can execute in distributed mode (execution delegated to remote nodes)</li>
	<li>client can execute in mixed local/distributed mode with adaptive load-balancing</li>
</ul>


<h3>Full fledged samples</h3>
<ul>
	<li><a href="samples-pack/Fractals/Readme.php">Mandelbrot / Julia set fractals generation</a></li>
	<li><a href="samples-pack/SequenceAlignment/Readme.php">Protein and DNA sequence alignment</a></li>
	<li><a href="samples-pack/WebSearchEngine/Readme.php">Distributed web crawler and search engine</a></li>
	<li><a href="samples-pack/TomcatPort/Readme.php">Tomcat 5.5/6.0 port</a></li>
	<li><a href="samples-pack/CustomMBeans/Readme.php">Pluggable management beans</a></li>
	<li><a href="samples-pack/DataEncryption/Readme.php">Network data encryption</a></li>
	<li><a href="samples-pack/StartupClasses/Readme.php">Customized server and node initialization</a></li>
	<li><a href="samples-pack/MatrixMultiplication/Readme.php">Basic dense matrix multiplication parallelization</a></li>
	<li><a href="samples-pack/DataDependency/Readme.php">Simulation of large portfolio updates</a></li>
	<li><a href="samples-pack/NodeTray/Readme.php">JPPF node health monitor in the system tray</a></li>
	<li><a href="samples-pack/CustomLoadBalancer/Readme.php">An example of a sophisticated load-balancer implementation</a></li>
	<li><a href="samples-pack/TaskNotifications/Readme.php">A customization that allows tasks to send notifications while executing</a></li>
	<li><a href="samples-pack/IdleSystem/Readme.php">An extension that enables nodes to run only when the machine is idle</a></li>
	<li><a href="samples-pack/NodeLifeCycle/Readme.php">Control of database transactions via node life cycle events</a></li>
	<li><a href="samples-pack/Nbody/Readme.php">Parallel N-body problem applied to anti-protons trapped in a  magnetic field</a></li>
	<li><a href="samples-pack/FTPServer/Readme.php">How to embed and use an FTP server in JPPF</a></li>
	<li><a href="samples-pack/NodeConnectionEvents/Readme.php">How to receive notifications of nodes connecting and disconnecting on the server</a></li>
	<li><a href="samples-pack/JobRecovery/Readme.php">Job recovery after an application crash</a></li>
	<li><a href="samples-pack/InitializationHook/Readme.php">Using a node initialization hook to implement a sophisticated failover mechanism</a></li>
	<li><a href="samples-pack/ExtendedClassLoading/Readme.php">Using the JPPF class loading extensions to automate the deployment and management of application libraries in the nodes at runtime</a></li>
</ul>

$template{name="about-page-footer"}$
