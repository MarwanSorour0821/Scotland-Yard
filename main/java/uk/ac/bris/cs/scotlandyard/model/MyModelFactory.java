

package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.LinkedList;

public final class MyModelFactory implements Factory<Model> {

	public final class MyModel implements Model {

		private Board.GameState state;
		private LinkedList<Observer> observers;

		MyModel(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
			this.state = new MyGameStateFactory().build(setup, mrX, detectives);
			this.observers = new LinkedList<>();
		}

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return state;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if (observer == null) {throw new NullPointerException("null observer.");}
			if(observers.contains(observer)){throw new IllegalArgumentException(" observer already registered.");}
			observers.add(observer);
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if (observer == null) {throw new NullPointerException("null observer so can't unregister");}
			if(!observers.contains(observer)) {throw new IllegalArgumentException("not registered.");}
			observers.remove(observer);
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(observers);
		}

		@Override
		public void chooseMove(@Nonnull Move move) {
			state = state.advance(move);

			if(!state.getWinner().isEmpty()){
				for (Observer x : observers){x.onModelChanged(state, Observer.Event.GAME_OVER);}
			}
			else{
				for(Observer x : observers){x.onModelChanged(state, Observer.Event.MOVE_MADE);}
			}
		}
	}

	@Nonnull @Override
	public Model build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new MyModel(setup, mrX, detectives);
	}
}
