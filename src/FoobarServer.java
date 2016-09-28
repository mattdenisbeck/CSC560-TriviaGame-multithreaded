/*
	Matthew Beck - CSC 560
	Program #2 - FOOBAR The Game (multithreaded ver.)
	Due: 4/22/2015
	Filename: FoobarServer.java
	This is the server side of the program. It takes a formatted game dated file and sends
	the questions and answers to the clients on port 8241. It accepts the clients' responses
	as strings and calculates the scores according to the rules detailed in the client.
	It allows 4 simultaneous connections.
	Server goes into an infinite loop, so that clients can reconnect without restarting the server.
	
	Path to my game data file, used for testing: /Users/matthewbeck/Desktop/foobargamedata.txt
 */

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class FoobarServer {

	private static int port = 8241, connections = 4; //connections must be greater than 0
	private static int activeConnections = 0; //records active number of active players
	private static Socket[] clientSockets = new Socket[connections];
	private static String[] playerIDs = new String[connections];
	private static int[] scores = new int[connections]; //records players final scores
	private static Semaphore semaphore = new Semaphore(1); //used to protect shared variable activeConnections

	public static void main(String[] args) {


		//Loading game data file
		String path;
		Scanner input = new Scanner(System.in);
		System.out.println("Enter the complete path of a game data file:");
		path = input.next();
		input.close();
		
		//starting infinite loop
		do {
			
			//Waiting for connection to the clients
			try { 
				
				//wait until there are no active client connections
				while (true) {
					semaphore.acquire();
					if (activeConnections == 0)
						break;
					semaphore.release();
				}
				semaphore.release();

				System.out.println("Foobar Server is waiting for a connection...");
				
				//Listening on port
				ServerSocket server = new ServerSocket(port);	

				//accept client connections
				int i = 0;
				while(i < connections) {
					clientSockets[i] = server.accept();
					activeConnections++;
					System.out.println("New client connected. Client Count = " + activeConnections);
					
					//accept playerIDs from clients
					BufferedReader in = new BufferedReader(new InputStreamReader(clientSockets[i].getInputStream()));
					playerIDs[i] = in.readLine();
					
					i++;
				}

				//span game threads for each client
				i = 0;
				while(i < connections) {
					File gamedata = new File(path);
					input = new Scanner(gamedata);
					GameServer game = new GameServer(clientSockets[i], input, playerIDs[i],i);
					Thread t = new Thread(game);
					t.start();
					i++;
				}
				server.close();
			}
			catch (FileNotFoundException e1) {
				System.err.print("File not found: ");
				System.err.println(e1.getMessage());
				System.exit(1);
			}
			catch (IOException e){
				System.out.println("Exception caught when trying to listen on port 8241 or listening for a connection");
				System.out.println(e.getMessage());
			} 
			catch (InterruptedException e) {
				System.err.print("Semaphore acquire interrupted");
				e.printStackTrace();
			} 
		} while(true);
	}

	static class GameServer implements Runnable {

		private Socket clientSocket;	//client connection
		private Scanner input;			//game data file
		private String playerID;		//player's ID
		private int index;				//order in which player connected to server

		GameServer(Socket c, Scanner s, String p, int i) {
			this.clientSocket = c;
			this.input = s;
			this.playerID = p;
			this.index = i;
		}

		@Override
		public void run() {
			
			//main game code
			//Declaring input/output streams
			try (
					PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
					BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					)
					
					{
				
				String clientAnswer = null;		//holds string sent from client
				int incorrectAnswer;			//stores incorrect answer from game data file
				int score;						//stores points scored for each question
				int total = 0;					//stores the final game score
				int qnumber = 1;				//keeps track of the question number

				out.println("All players connected. Let's begin!");
				
				while (input.hasNext()) {

					//Sending the next question to the client
					out.println("Question " + qnumber + ": " + input.nextLine());

					//Sending the answer choices to the client
					out.println("1. " + input.nextLine());
					out.println("2. " + input.nextLine());
					out.println("3. " + input.nextLine());
					out.println("4. " + input.nextLine());

					//Reading incorrect answer from game data file
					incorrectAnswer = Integer.parseInt(input.nextLine());

					//Receiving submitted answer from the client
					clientAnswer = in.readLine();
					System.out.println(playerID + " answered: " + clientAnswer);		//server side output to verify client submission

					//Scoring the client's submitted answer
					score = 0;

					//Keeps track of duplicate answers. If index i is set to true, then the scoring logic already read in an i from the client.
					//Index 0 == true, so that I can treat the array as if index 1 is the first index in the array.
					boolean[] used = {true, false, false, false, false}; 

					//Iterates through client submission, adds 250 to score for correct answers, subtracts 250 for incorrect answers.
					//A score of -250 is possible.
					//Updates used[] so as to skip over duplicate submissions.
					//Try-Catch block is used to ignore non-numerical input.
					for(int i = 0; i < clientAnswer.length(); i++) {
						try {
							int answer = Integer.parseInt(clientAnswer.substring(i, i+1));

							if (answer == 1 || answer == 2 || answer == 3 || answer == 4) {
								if ( answer != incorrectAnswer && used[answer] == false) {
									score += 250;
									used[answer] = true;
								}
								if ( answer == incorrectAnswer && used[answer] == false) {
									score -=250;
									used[answer] = true;
								}
							}
						}
						catch (NumberFormatException e) {
							continue;
						}
					}

					//Updating the final game score
					total += score;

					//Sending the incorrect answer to the client.
					out.println("The incorrect answer was: #" + incorrectAnswer);

					//Sending the question score and current game score to the client.
					out.println("On this question you scored " + score + " points.");
					out.println("Your current total: " + total + " points.");

					qnumber++;
				}

				//After all 10 questions, sending final score to client.
				scores[index] = total;
				out.println("Game over. Your final score: " + total + " points.");
				
				//game is finished to decrement activeConnections
				semaphore.acquire();
				activeConnections--;
				semaphore.release();
				
				//wait until all active clients have finished the game
				while (true) {
					semaphore.acquire();
					if (activeConnections == 0)
						break;
					semaphore.release();
				}
				semaphore.release();
				
				//send final scores to clients
				out.println("Final Scores:");
				out.println(playerIDs[0] + ": " + scores[0]);
				out.println(playerIDs[1] + ": " + scores[1]);
				out.println(playerIDs[2] + ": " + scores[2]);
				out.println(playerIDs[3] + ": " + scores[3]);

					}
			
			catch (IOException e){
				System.out.println("Exception caught when trying to create I/O streams on clientSocket");
				System.out.println(e.getMessage());
			} 
			catch (InterruptedException e) {
				System.err.print("Semaphore acquire interrupted");
				e.printStackTrace();
			} 
		}
	}
}


