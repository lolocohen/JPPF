<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Parallel N-body Sample"}$

<div align="justify">

					<h3>What does the sample do?</h3>
					<p>This sample is a parallel implementation of the <a href="http://en.wikipedia.org/wiki/N-body_problem" alt="N_body on Wikipedia">N-body problem</a> for simulating the motion of anti-protons trapped in a magnetic field.<br>
					It displays a real-time graphical simulation of the movement of the anti-protons, based on the parameters specified in the configuration file.<br/>
					<p>The source in this sample is based on the <a href="http://www.cs.rit.edu/~ark/pj/doc/index.html?edu/rit/clu/antimatter/package-summary.html">examples</a> provided in the <a href="http://www.cs.rit.edu/~ark/pj.shtml">Java Parallel Library</a>.
					<p>Here's a screenshot of the simulation in action:
					<p><img src="images/Nbody.gif" border="0">

					<h3>How do I run it?</h3>
					Before running this sample application, you need to install a JPPF server and at least one node.<br>
					For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/doc/v4/index.php?title=Introduction">JPPF documentation</a>.<br>
					Once you have installed a server and node, perform the following steps:
					<ol class="samplesList">
						<li>open a command prompt in JPPF-x.y-samples-pack/Nbody</li>
						<li>to build the sample: type "<b>ant compile</b>" or simply "<b>ant</b>"; this will compile all source files in the sample</li>
						<li>to run the simulation, you can either use the batch script "run.bat" (on Windows) or "run.sh" (on Linux), or the Ant script: "ant run"</li>
					</ol>
					<p>You might also want to play with the simulation parameters to see how they impact the motion of the simulated antiprotons. They are specified in the configuration file <b>config/jppf.properties</b> as follows:
<pre class="samples">
<font color="green"># charge on an antiproton</font>
nbody.qp = 6
<font color="green"># magnetic field strength</font>
nbody.b = 0.42
<font color="green"># simulation radius (size of the graphical panel)</font>
nbody.radius = 300
<font color="green"># granularity of the time steps</font>
nbody.dt = 0.01
<font color="green"># number of bodies (anti protons)</font>
nbody.n = 100
<font color="green"># number of time steps</font>
nbody.time.steps = 13000
<font color="green"># How many bodies (anti protons) per JPPF task</font>
nbody.bodies.per.task = 25
</pre>

					<h3>I have additional questions and comments, where can I go?</h3>
					<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>CustomMBeans/src</b> folder.
					<p>In addition, There are 2 privileged places you can go to:
					<ul class="samplesList">
						<li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
						<li><a href="http://www.jppf.org/doc/v4/">The JPPF documentation</a></li>
					</ul>
					
</div>

$template{name="about-page-footer"}$
