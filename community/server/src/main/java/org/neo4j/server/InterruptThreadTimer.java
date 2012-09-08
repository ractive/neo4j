/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Interrupts a thread after a given timeout, can be cancelled if needed.
 */
public abstract class InterruptThreadTimer 
{
	
	public static class InterruptThreadTask extends TimerTask
	{
		private Thread threadToInterrupt;
		private boolean wasExecuted = false;

		public InterruptThreadTask(Thread threadToInterrupt)
		{
			this.threadToInterrupt = threadToInterrupt;
		}
		
		@Override
		public void run() {
			wasExecuted = true;
			threadToInterrupt.interrupt();
		}
		
		public boolean wasExecuted()
		{
			return this.wasExecuted;
		}
	}

	public static InterruptThreadTimer createTimer(long timeoutMillis, Thread threadToInterrupt)
	{
		return new ActualInterruptThreadTimer(timeoutMillis, threadToInterrupt);
	}

	public static InterruptThreadTimer createNoOpTimer()
	{
		return new NoOpInterruptThreadTimer();
	}
	
	private static class ActualInterruptThreadTimer extends InterruptThreadTimer
	{
		private Timer timer = new Timer();
		private final InterruptThreadTask task;
		private long timeout;
		
		public ActualInterruptThreadTimer(long timeoutMillis, Thread threadToInterrupt)
		{
			this.task = new InterruptThreadTask(threadToInterrupt);
			this.timeout = timeoutMillis;
		}
	
		@Override
		public void startCountdown()
		{
			timer.schedule(task, timeout);
		}
		
		@Override
		public void stopCountdown()
		{
			timer.cancel();
		}
		
		@Override
		public boolean wasTriggered()
		{
			return task.wasExecuted();
		}

		@Override
		public long getTimeoutMillis() {
			return timeout;
		}
	}
	
	private static class NoOpInterruptThreadTimer extends InterruptThreadTimer
	{
		
		public NoOpInterruptThreadTimer()
		{
		}
	
		@Override
		public void startCountdown()
		{
			
		}
		
		@Override
		public void stopCountdown()
		{
			
		}
		
		@Override
		public boolean wasTriggered()
		{
			return false;
		}

		@Override
		public long getTimeoutMillis() {
			return 0;
		}
	}
	
	public abstract void startCountdown();
	public abstract void stopCountdown();
	public abstract boolean wasTriggered();
	public abstract long getTimeoutMillis();
	
}