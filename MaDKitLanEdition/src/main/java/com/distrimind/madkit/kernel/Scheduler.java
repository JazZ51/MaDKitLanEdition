/*
 * MadKitLanEdition (created by Jason MAHDJOUB (jason.mahdjoub@distri-mind.fr)) Copyright (c)
 * 2015 is a fork of MadKit and MadKitGroupExtension. 
 * 
 * Copyright or © or Copr. Jason Mahdjoub, Fabien Michel, Olivier Gutknecht, Jacques Ferber (1997)
 * 
 * jason.mahdjoub@distri-mind.fr
 * fmichel@lirmm.fr
 * olg@no-distance.net
 * ferber@lirmm.fr
 * 
 * This software is a computer program whose purpose is to
 * provide a lightweight Java library for designing and simulating Multi-Agent Systems (MAS).
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package com.distrimind.madkit.kernel;

import static com.distrimind.madkit.kernel.Scheduler.SimulationState.PAUSED;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.LinkedHashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.distrimind.madkit.action.SchedulingAction;
import com.distrimind.madkit.gui.SwingUtil;
import com.distrimind.madkit.message.SchedulingMessage;

/**
 * This class defines a generic threaded scheduler agent. It holds a collection
 * of activators. The default state of a scheduler is
 * {@link SimulationState#PAUSED}. The default delay between two steps is 0 ms
 * (max speed).
 * 
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 2.0
 * @since MadkitLanEdition 1.0
 * @version 5.3
 * @see Activator
 */
public class Scheduler extends Agent {

	/**
	 * A simulation state. The simulation process managed by a scheduler agent can
	 * be in one of the following states:
	 * <ul>
	 * <li>{@link #RUNNING}<br>
	 * The simulation process is running normally.</li>
	 * <li>{@link #STEP}<br>
	 * The scheduler will process one simulation step and then will be in the
	 * {@link #PAUSED} state.</li>
	 * <li>{@link #PAUSED}<br>
	 * The simulation is paused. This is the default state.</li>
	 * </ul>
	 * 
	 * @author Fabien Michel
	 * @since MaDKit 5.0
	 * @see #getSimulationState
	 */
	public enum SimulationState {

		/**
		 * The simulation process is running normally.
		 */
		RUNNING,

		/**
		 * The scheduler will process one simulation step and then will be in the
		 * {@link #PAUSED} state.
		 * 
		 */
		STEP,

		/**
		 * The simulation is paused.
		 */
		PAUSED,

		/**
		 * The simulation is ending
		 */
		SHUTDOWN
	}

	SimulationState simulationState = SimulationState.PAUSED;

	final private Set<Activator<? extends AbstractAgent>> activators = new LinkedHashSet<>();

	private Action run, step, speedUp, speedDown;

	// private JLabel timer;
	private int delay=400;

	/**
	 * specify the delay between 2 steps
	 */
	@SuppressWarnings("serial")
	private final DefaultBoundedRangeModel speedModel = new DefaultBoundedRangeModel(400, 0, 0, 400) {

		public void setValue(int n) {
			super.setValue(n);
			delay = 400 - getValue();
		}
	};

	/**
	 * Returns the delay between two simulation steps
	 * 
	 * @return the delay between two simulation steps.
	 */
	public int getDelay() {
		return delay;
	}

	/**
	 * Sets the delay between two simulation steps. That is the pause time between
	 * to call to {@link #doSimulationStep()}. The value is automatically adjusted
	 * between 0 and 400.
	 * 
	 * @param delay
	 *            the pause between two steps in milliseconds, an integer between 0
	 *            and 400: O is max speed.
	 */
	public void setDelay(final int delay) {
		speedModel.setValue(speedModel.getMaximum() - delay);
	}

	private double GVT = 0; // simulation global virtual time

	/**
	 * Returns the simulation global virtual time.
	 * 
	 * @return the gVT
	 */
	public double getGVT() {
		return GVT;
	}

	/**
	 * Sets the simulation global virtual time.
	 * 
	 * @param GVT
	 *            the actual simulation time
	 */
	public void setGVT(final double GVT) {
		this.GVT = GVT;
		if (gvtModel != null) {
			gvtModel.notifyObservers((int) GVT);
		}
	}

	private double simulationDuration;

	private GVTModel gvtModel;

	/**
	 * This constructor is equivalent to <code>Scheduler(Double.MAX_VALUE)</code>
	 */
	public Scheduler() {
		this(Double.MAX_VALUE);
	}

	// public Scheduler(boolean multicore) {
	// this(0, Double.MAX_VALUE);
	// }

	/**
	 * Constructor specifying the time at which the simulation ends.
	 * 
	 * @param endTime
	 *            the GVT at which the simulation will automatically stop
	 */
	public Scheduler(final double endTime) {
		buildActions();
		setSimulationDuration(endTime);
	}

	/**
	 * Setup the default Scheduler GUI when launched with the default MaDKit GUI
	 * mechanism.
	 * 
	 * @see com.distrimind.madkit.kernel.AbstractAgent#setupFrame(javax.swing.JFrame)
	 * @since MaDKit 5.0.0.8
	 */
	@Override
	public void setupFrame(JFrame frame) {
		super.setupFrame(frame);
		frame.add(getSchedulerToolBar(), BorderLayout.PAGE_START);
		frame.add(getSchedulerStatusLabel(), BorderLayout.PAGE_END);
		setGVT(GVT);
		frame.getJMenuBar().add(getSchedulerMenu(), 2);
	}

	/**
	 * Adds an activator to the kernel engine. This has to be done to make an
	 * activator work properly
	 * 
	 * @param activator
	 *            an activator.
	 */
	public void addActivator(final Activator<? extends AbstractAgent> activator) {
		if (kernel.addOverlooker(this, activator)) {
			activators.add(activator);
			if (logger != null && logger.isLoggable(Level.FINE))
				logger.fine("Activator added: " + activator);
		} else if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("Impossible to add activator : " + activator);

	}

	/**
	 * Removes an activator from the kernel engine.
	 * 
	 * @param activator
	 *            an activator.
	 */
	public void removeActivator(final Activator<? extends AbstractAgent> activator) {
		kernel.removeOverlooker(this, activator);
		activators.remove(activator);
		if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("Activator removed: " + activator);
	}

	/**
	 * Executes all the activators in the order they have been added, using
	 * {@link Activator#execute(Object...)}, and then increments the global virtual
	 * time of this scheduler by one unit.
	 * 
	 * This also automatically calls the multicore mode of the activator if it is
	 * set so. This method should be overridden to define customized scheduling
	 * policy. So default implementation is :
	 * 
	 * <pre>
	 * 
	 * 	public void doSimulationStep() {
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;if (logger != null) {
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;logger.finer("Doing simulation step " + GVT);
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;}
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;for (final Activator&lt;? extends AbstractAgent&gt; activator : activators) 
	 * 		{
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;if (logger != null)
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;logger.finer("Activating\n--------&gt; " + activator);
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;activator.execute();
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;
	 * 		}
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;setGVT(getGVT() + 1);
	 * 		}
	 * </pre>
	 */
	public void doSimulationStep() {
		if (logger != null && logger.isLoggable(Level.FINER)) {
			logger.finer("Doing simulation step " + GVT);
		}
		for (final Activator<? extends AbstractAgent> activator : activators) {
			if (logger != null && logger.isLoggable(Level.FINER))
				logger.finer("Activating --------> " + activator);
			// try {
			activator.execute();
			// } catch (SimulationException e) {//TODO is it better ?
			// setSimulationState(SimulationState.SHUTDOWN);
			// getLogger().log(Level.SEVERE, e.getMessage(), e);
			// }
		}
		setGVT(GVT + 1);
	}

	@Override
	protected void end() {
		simulationState = PAUSED;
		if (logger != null)
			logger.info("Simulation stopped !");
	}

	/**
	 * The state of the simulation.
	 * 
	 * @return the state in which the simulation is.
	 * @see SimulationState
	 */
	public SimulationState getSimulationState() {
		return simulationState;
	}

	/**
	 * Changes the state of the scheduler
	 * 
	 * @param newState
	 *            the new state
	 */
	protected void setSimulationState(final SimulationState newState) {// TODO proceedEnumMessage
		if (simulationState != newState) {
			simulationState = newState;
			if (logger!=null)
				logger.log(Level.FINE, "New simulation state : "+simulationState);
			switch (simulationState) {
			case STEP:case PAUSED:
					run.setEnabled(true);
				break;
			case RUNNING:case SHUTDOWN:
					run.setEnabled(false);
			break;
			default:// impossible
				logLifeException(new Exception("state not handle : " + newState.toString()));
			}
		}
	}

	/**
	 * Scheduler's default behavior.
	 * 
	 * 
	 * 
	 * @throws InterruptedException if the current thread was interrupted
	 * 
	 * @see com.distrimind.madkit.kernel.Agent#liveCycle()
	 */
	@Override
	protected void liveCycle() throws InterruptedException {
		if (GVT > simulationDuration) {
			if (logger != null)
				logger.info("Quitting: Simulation has reached end time " + simulationDuration);
			this.killAgent(this);
		}
		pause(delay);
		checkMail(nextMessage());
		switch (simulationState) {
		case RUNNING:
			doSimulationStep();
			break;
		case PAUSED:
			paused();
			break;
		case STEP:
			simulationState = PAUSED;
			doSimulationStep();
			break;
		case SHUTDOWN:
			this.killAgent(this);
		default:
			getLogger().severe("state not handled " + simulationState);
		}
	}

	/**
	 * Changes my state according to a {@link SchedulingMessage} and sends a reply
	 * to the sender as acknowledgment.
	 * 
	 * @param m
	 *            the received message
	 */
	protected void checkMail(final Message m) {
		if (m != null) {
			try {
				SchedulingAction code = ((SchedulingMessage) m).getCode();
				switch (code) {
				case RUN:
					setSimulationState(SimulationState.RUNNING);
					break;
				case STEP:
					setSimulationState(SimulationState.STEP);
					break;
				case PAUSE:
					setSimulationState(SimulationState.PAUSED);
					break;
				case SHUTDOWN:
					setSimulationState(SimulationState.SHUTDOWN);
					break;
				case SPEED_UP:
					speedModel.setValue(speedModel.getValue() - 50);
					break;
				case SPEED_DOWN:
					speedModel.setValue(speedModel.getValue() + 50);
					break;
				}
				if (m.getSender() != null) {
					sendReply(m, m);
				}
			} catch (ClassCastException e) {
				if (logger != null)
					logger.info("I received a message that I cannot understand" + m);
			}
		}
	}

	/**
	 * Runs {@link #checkMail(Message)} every 1000 ms.
	 * 
	 * @throws InterruptedException if the current thread was interrupted
	 */
	protected void paused() throws InterruptedException {

		checkMail(waitNextMessage(1000));
	}

	/**
	 * @see com.distrimind.madkit.kernel.AbstractAgent#terminate()
	 */
	@Override
	final void terminate() {
		removeAllActivators();
		super.terminate();
	}

	/**
	 * Remove all the activators which have been previously added
	 */
	public void removeAllActivators() {
		for (final Activator<? extends AbstractAgent> a : activators) {
			kernel.removeOverlooker(this, a);
		}
		activators.clear();
	}

	/**
	 * Sets the simulation time for which the scheduler should end the simulation.
	 * 
	 * @param endTime
	 *            the end time to set
	 */
	public void setSimulationDuration(final double endTime) {
		this.simulationDuration = endTime;
	}

	/**
	 * @return the simulationDuration
	 */
	public double getSimulationDuration() {
		return simulationDuration;
	}

	private void buildActions() {
		run = SchedulingAction.RUN.getActionFor(this);
		step = SchedulingAction.STEP.getActionFor(this);
		speedUp = SchedulingAction.SPEED_UP.getActionFor(this);
		speedDown = SchedulingAction.SPEED_DOWN.getActionFor(this);
	}

	/**
	 * Returns a toolbar which could be used in any GUI.
	 * 
	 * @return a toolBar controlling the scheduler's actions
	 */
	public JToolBar getSchedulerToolBar() {
		final JToolBar toolBar = new JToolBar("scheduler toolbar");
		toolBar.add(run);
		toolBar.add(step);
		final JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBorder(new TitledBorder("speed"));
		final JSlider sp = new JSlider(speedModel);
		sp.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int move = -e.getWheelRotation();
				if (sp.getValue() < 398) {
					move *= 10;
				}
				move = Math.min((move + sp.getValue()), sp.getMaximum());
				sp.setValue(move);
				sp.getChangeListeners()[0].stateChanged(new ChangeEvent(this));
			}
		});
		sp.addChangeListener(e -> updateToolTip(p, sp));
		updateToolTip(p, sp);
		// p.setPreferredSize(new Dimension(150, 25));
		p.add(sp);
		// toolBar.addSeparator();
		// toolBar.add(Box.createRigidArea(new Dimension(40,5)));
		// toolBar.add(Box.createHorizontalGlue());
		toolBar.add(p);
		// toolBar.add(getGVTLabel());
		SwingUtil.scaleAllAbstractButtonIconsOf(toolBar, 24);
		return toolBar;
	}


	void updateToolTip(final JPanel p, final JSlider sp) {
		final String text = "pause = " + getDelay() + " ms";
		sp.setToolTipText(text);
		p.setToolTipText(text);
	}

	/**
	 * Returns a menu which could be used in any GUI.
	 * 
	 * @return a menu controlling the scheduler's actions
	 */
	public JMenu getSchedulerMenu() {
		JMenu myMenu = new JMenu("Scheduling");
		myMenu.setMnemonic(KeyEvent.VK_S);
		myMenu.add(run);
		myMenu.add(step);
		myMenu.add(speedUp);
		myMenu.add(speedDown);
		return myMenu;
	}

	/**
	 * Returns a label giving some information on the simulation process
	 * 
	 * @return a label giving some information on the simulation process
	 */
	public JLabel getSchedulerStatusLabel() {
		if (gvtModel == null) {
			gvtModel = new GVTModel();
		}
		@SuppressWarnings("serial")
		GVTJLabel timer = new GVTJLabel() {
			@Override
			public void update(Observable o, Object arg) {
				setText("Simulation " + simulationState + ", time is " + arg);
			}
		};
		timer.setText("GVT");
		gvtModel.addObserver(timer);
		timer.setBorder(new EmptyBorder(4, 4, 4, 4));
		timer.setHorizontalAlignment(SwingConstants.LEADING);
		setGVT(getGVT());
		return timer;
	}

	/**
	 * Returns a label giving the simulation time
	 * 
	 * @return a label giving the simulation time
	 */
	public JLabel getGVTLabel() {
		if (gvtModel == null) {
			gvtModel = new GVTModel();
		}
		final GVTJLabel timer = new GVTJLabel();
		timer.setText("0");
		gvtModel.addObserver(timer);
		timer.setBorder(new EmptyBorder(4, 4, 4, 4));
		timer.setHorizontalAlignment(SwingConstants.LEADING);
		setGVT(getGVT());
		return timer;
	}

}

final class GVTModel extends Observable {
	@Override
	public void notifyObservers(Object arg) {
		setChanged();
		super.notifyObservers(arg);
	}
}

class GVTJLabel extends JLabel implements Observer {

	private static final long serialVersionUID = 2320718202738802489L;

	@Override
	public void update(Observable o, Object arg) {
		setText(arg.toString());
	}

}
