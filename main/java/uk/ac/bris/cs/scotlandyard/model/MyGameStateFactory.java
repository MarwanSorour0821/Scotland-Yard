package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;

import java.util.*;


/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull
	@Override
	public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		testInput(setup, mrX, detectives);
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

	private void testInput(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {

		if (setup.moves.isEmpty()) throw new IllegalArgumentException("moves is empty");
		if (mrX == null) throw new NullPointerException("mrX is null");
		if (detectives.isEmpty()) throw new IllegalArgumentException("detectives is empty");
		if (setup.graph.edges().isEmpty()) throw new IllegalArgumentException("empty graph");

		//check duplicate properties for detective
		for (int i = 0; i < detectives.size(); i++) {
			for (int j = i + 1; j < detectives.size(); j++) {
				// if the next detective doest exist so break
				if (j > detectives.size()) break;
				if (detectives.get(i).location() == detectives.get(j).location()) {
					throw new IllegalArgumentException("Same location!");
				}
				if (detectives.get(i).piece() == detectives.get(j).piece()) {
					throw new IllegalArgumentException("Duplicated game pieces!");
				}
			}
		}

		// tests if detectives have the correct tickets and if they are a detective
		for (Player pd : detectives) {
			if (!pd.isDetective()) throw new IllegalArgumentException("No detective!");
			if (pd.has(ScotlandYard.Ticket.DOUBLE)) throw new IllegalArgumentException("detectives has double");
			if (pd.has(ScotlandYard.Ticket.SECRET)) throw new IllegalArgumentException("detectives has secret");
		}

	}

	private final class MyGameState implements GameState {

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;


		private MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining,
							final ImmutableList<LogEntry> log,
							final Player mrX,
							final List<Player> detectives) {

			this.setup = setup;
			this.mrX = mrX;
			this.detectives = detectives;
			this.remaining = remaining;
			this.log = log;
			this.moves = getAvailableMoves();
			this.winner = getWinner();

		}

		@Override
		public GameSetup getSetup() {
			return setup;
		}

		@Override
		public ImmutableSet<Piece> getPlayers() {

			//making a new set and adding all players to it
			HashSet<Piece> ply = new HashSet<>();
			ply.add(mrX.piece());
			for (Player player : detectives) {
				player.piece();
				ply.add(player.piece());
			}
			return ImmutableSet.copyOf(ply);
		}

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {

			//Iterates through detectives to obtain location
			for (Player player : detectives) {
				if (player.piece() == detective) return Optional.of(player.location());
			}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {

			List<Player> allPlayers = new ArrayList<>(detectives);
			allPlayers.add(mrX);

			//New player to use as a condition
			Player player1 = null;

			for(Player p2 : allPlayers){
				if(p2.piece().equals(piece)){
					player1 = p2;
					break;
				}
			}

			if(player1 == null) return Optional.empty();

			else{
				Player p = player1;
				return Optional.of(new TicketBoard() {
					@Override
					public int getCount(@Nonnull Ticket ticket) {
						return p.tickets().get(ticket);
					}
				});
			}
		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {return log;}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {

			//Use for loop to obtain pieces of detectives
			Set<Piece> detPiece = new HashSet();
			for(Player pd : detectives){
				detPiece.add(pd.piece());

				//check if any of the pieces have the same location as mrX
				//Detectives win if condition is satisfied
				if(pd.location() == mrX.location()) return ImmutableSet.copyOf(detPiece);
			}

			//check if mrX has any more available moves, if not then detectives win
			if(makeSingleMoves(setup,detectives, mrX, mrX.location()).isEmpty() &&
					makeDoubleMoves(setup, detectives, mrX, mrX.location()).isEmpty() &&
					remaining.contains(mrX.piece())){
				return ImmutableSet.copyOf(detPiece);
			}

			//check if number of moves mrX has made is equal to log size, if true then mrX wins
			else if(getMrXTravelLog().size() == setup.moves.size() && remaining.contains(mrX.piece())) return ImmutableSet.of(mrX.piece());

			//checks if any of the detectives can make a move, if not then mrX wins
			int noMove = 0;

			for (Player p : detectives){
				if(makeSingleMoves(setup, detectives, p, p.location()).isEmpty()
						&& makeDoubleMoves(setup, detectives, p, p.location()).isEmpty()){
					noMove = noMove + 1;
				}
			}

			//if all detectives can't make a move then mrX wins
			if(noMove == detectives.size()) return ImmutableSet.of(mrX.piece());

			return ImmutableSet.of();

		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			HashSet<Move> mov = new HashSet<>();

			//if mrX is in the remaining set then check his available moves
			if (remaining.contains(mrX.piece())) {
				mov.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));

				//in order to use double move, number of moves needs to be less than log size by at least 2
				if (setup.moves.size() - 2 >= log.size() && mrX.has(Ticket.DOUBLE)) {
					mov.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
				}
			}

			//if any of detectives are in remaining set then check their available moves
			else {
				for (Player pl : detectives) {
					if (remaining.contains(pl.piece())) {mov.addAll(makeSingleMoves(setup, detectives, pl, pl.location()));}}
			}

			//if there is a winner then just return Immutable set
			if (!getWinner().isEmpty()) {return ImmutableSet.of();}

			return ImmutableSet.copyOf(mov);
		}

		@Override
		public @NotNull GameState advance(Move move) {
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);

			List<LogEntry> Log1 = new ArrayList<>(log);
			List<Player> newDet = new ArrayList<>();
			List<Piece> newRem = new ArrayList<>();
			Player newMrx;

			// To move player we use the visitor pattern
			Visitor<Player> v = new Visitor<>() {

				@Override
				public Player visit(SingleMove move) {

					// check if player is either a detective or mrX
					Player player = obtainPlayerPiece(move.commencedBy());

					// move player to next position and use their ticket
					Player newPlayer = player.use(move.ticket).at(move.destination);

					if (!player.isMrX()) {
						// when a detective uses a ticket, it gives mrX that ticket
						mrX = mrX.give(move.ticket);
					} else {
						// check if mrX needs to reveal his move
						if(!setup.moves.get(log.size())){
							Log1.add(LogEntry.hidden(move.ticket));
						}
						else{
							Log1.add(LogEntry.reveal(move.ticket, move.destination));
						}
					}
					return newPlayer;
				}

				@Override
				public Player visit(DoubleMove move) {
					Player player = obtainPlayerPiece(move.commencedBy());

					//use ticket and move to the next position
					Player newPlayer = player.use(move.tickets());
					newPlayer = newPlayer.at(move.destination2);


					//checks if mrX needs to reveal his move
					if (setup.moves.get(log.size())) {
						Log1.add(LogEntry.reveal(move.ticket1, move.destination1));
					} else {
						Log1.add(LogEntry.hidden(move.ticket1));
					}

					//need to check again to see if he needs to reveal his move on
					// either of the 2 moves of double moves
					if (setup.moves.get(Log1.size())) {
						Log1.add(LogEntry.reveal(move.ticket2, move.destination2));
					} else {
						Log1.add(LogEntry.hidden(move.ticket2));
					}

					return newPlayer;
				}
			};


			Player newPlayer = move.accept(v);



			for (Player p : detectives) {
				if (p.piece() == newPlayer.piece()) {
					newDet.add(newPlayer);
				} else {
					newDet.add(p);
				}
			}


			for (Piece p : remaining) {
				if (p != newPlayer.piece()) {
					newRem.add(p);
				}
			}


			if (newPlayer.isMrX()) {
				newMrx = newPlayer;
			} else {newMrx = mrX;}

			for(Piece p : remaining) {
				Player player = obtainPlayerPiece(p);
				if(p.isMrX()) {
					if(makeSingleMoves(setup, newDet, player, player.location()).isEmpty()
							&& !makeDoubleMoves(setup, newDet, newMrx, newMrx.location()).isEmpty() ) {
						newRem.add(newMrx.piece());
					}
				} else {

					if(makeSingleMoves(setup, newDet, player, player.location()).isEmpty()) {
						newRem.add(newMrx.piece());
					}
				}
			}

			if (move.commencedBy().isMrX()) {
				for (Player p : detectives) {newRem.add(p.piece());}
			} else {for (Piece p : remaining) {if (p != move.commencedBy()) newRem.add(p);}
			}

			if (newRem.isEmpty()) newRem.add(newMrx.piece());

			return new MyGameState(
					setup,
					ImmutableSet.copyOf(newRem),
					ImmutableList.copyOf(Log1),
					newMrx,
					newDet
			);

		}

		//HELPER FUNCTIONS

		private Player obtainPlayerPiece(Piece piece) {
			HashSet<Player> allPlayers = new HashSet<>(detectives);
			allPlayers.add(mrX);

			for (Player player : allPlayers) {
				if (player.piece().equals(piece)) return player;
			}
			return null;
		}

		public static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {

			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			HashSet<SingleMove> sinMov = new HashSet<>();

			for (int destination : setup.graph.adjacentNodes(source)) {
				// TODO find out if destination is occupied by a detective
				boolean flag = false;
				for (var location : detectives) {
					if (location.location() == destination) {
						flag = true;
						break;
					}
				}

				if (!flag) {

					for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {

						if (player.has(t.requiredTicket())) {
							SingleMove move = new SingleMove(player.piece(), source, t.requiredTicket(), destination);
							sinMov.add(move);
						}

						if (player.has(Ticket.SECRET)) {
							SingleMove move = new SingleMove(player.piece(), source, Ticket.SECRET, destination);
							sinMov.add(move);
						}
					}
				}
			}
			// TODO return the collection of moves
			return sinMov;
		}

		private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
			HashSet<DoubleMove> DoubleMove = new HashSet<>();
			Set<SingleMove> move = makeSingleMoves(setup, detectives, player, source);

			for (SingleMove moves : move) {
				Player p = new Player(player.piece(), player.tickets(), moves.destination).use(moves.ticket);
				Set<SingleMove> move2 = makeSingleMoves(setup, detectives, p, moves.destination);
				for (SingleMove ticket2 : move2) {
					DoubleMove.add(new DoubleMove(p.piece(), source, moves.ticket, moves.destination, ticket2.ticket, ticket2.destination));

				}
			}
			return DoubleMove;

		}

	}
}

