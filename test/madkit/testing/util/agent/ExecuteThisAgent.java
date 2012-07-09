/*
 * Copyright 1997-2011 Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * 
 * This file is part of MadKit.
 * 
 * MadKit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MadKit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with MadKit. If not, see <http://www.gnu.org/licenses/>.
 */
package madkit.testing.util.agent;

import static madkit.kernel.AbstractAgent.ReturnCode.SUCCESS;
import static madkit.kernel.JunitMadKit.COMMUNITY;
import static madkit.kernel.JunitMadKit.GROUP;
import static madkit.kernel.JunitMadKit.ROLE;
import static org.junit.Assert.assertEquals;
import madkit.kernel.Agent;

/**
 * @author Fabien Michel
 * @since MadKit 5.0.0.12
 * @version 0.9
 * 
 */
public class ExecuteThisAgent extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ExecuteThisAgent() {
	}

	@Override
	protected void live() {
		assertEquals(SUCCESS, createGroup(COMMUNITY, GROUP));
		assertEquals(SUCCESS, requestRole(COMMUNITY, GROUP, ROLE));
		pause(1000);
	}
	
	public static void main(String[] args) {
		executeThisAgent();
		executeThisAgent(1,true);
		executeThisAgent(1,false);
	}

}
