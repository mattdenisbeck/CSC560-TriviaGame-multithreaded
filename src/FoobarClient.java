/*
	Matthew Beck - CSC 560
	Program #2 - FOOBAR The Game (multithreaded ver.)
	Due: 4/22/2015
	Filename: FoobarClient.java
	This is the client side of the program. It receives and prints questions and choices
	from the server on port 8241. It accepts user input for answers and sends it as a string to 
	the server where it is scored according to the rules detailed at the beginning of the game.
	This is a multiplayer game. The game will not begin until all 4 players have connected.
 */

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class FoobarClient {

	public static void main(String[] args) {

		//Printing game rules, and asking player to indicate that he/she is ready to begin
		System.out.println("==========================================FOOBAR: The Game==========================================");
		System.out.print("Rules: You will be presented with 10 questions, each with 4 choices numbered 1-4.\n"
				+ "All but 1 of the answers are correct. For each question enter the numbers of the 3 correct answers.\n"
				+ "For example, if the incorrect answer is #2, you should enter 134 to receive full points.\n"
				+ "NOTE: Order does not matter. I.E. 134 is scored the same as 431.\n"
				+ "For each correct answer you will be awarded 250 pts. You will lose 250 pts for an incorrect answer.\n"
				+ "You will have 15 seconds to answer each question.\n"
				+ "This is a multiplayer game. The game will not begin until all players have connected.\n");
		System.out.print("====================================================================================================\n\n");
		System.out.print("Type in your name and hit enter when you are ready to begin: ");
		Scanner start = new Scanner(System.in);
		String name = start.nextLine();
		if (name != null) {
			System.out.println("Let's play, " + name + "!\n");
		}

		//Making a connection to the server
		System.out.println("Client is attempting to make a connection to the server...\n");		
		try (
				Socket toserverSocket = new Socket("localhost", 8241);

				//Declaring input/output streams
				PrintWriter out = new PrintWriter(toserverSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(toserverSocket.getInputStream()));
				BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
				) 
			{
			
			//sending playerID to server
			out.println(name);
			
			System.out.println("Waiting for other players to connect...\n");
			
			//printing message from server that all players are connected and the game is about to begin
			System.out.println(in.readLine() + "\n");
			
			Thread.sleep(3000); //pause game so player can get ready for first question
			
			int i = 0;
			while (i < 10) {

				//Receive and print question and answer choices
				System.out.println(in.readLine());
				System.out.println(in.readLine());
				System.out.println(in.readLine());
				System.out.println(in.readLine());
				System.out.println(in.readLine());

				//Getting answer from user input
				System.out.print("Answer: ");
				String answer = "";

				//Busy loop for question timer
				int x = 15; 	// set time to 15 seconds
				long startTime = System.currentTimeMillis();

				//Timer loop will exit if user submits answer or 15 seconds pass
				while ((System.currentTimeMillis() - startTime) < x * 1000
						&& !input.ready()) {
				}

				//If user submitted an answer, it is read into a variable and output back to the user.
				if (input.ready()){
					answer = input.readLine();
					System.out.println("\nYou entered: " + answer);
				} 

				//If time runs out, print a notification to the user.
				else {
					System.out.println("\n\nTime is up. You did not give an answer.");
				}

				//Sending the answer to the server. If time ran out, this will be an empty string, which will receive a score of 0.
				out.println(answer);

				//Receive and print the incorrect answer from the server.
				System.out.println(in.readLine());

				//Receive and print the question score and current game score from the server.
				System.out.println(in.readLine());
				System.out.println(in.readLine());
				System.out.print("======================================\n\n");

				Thread.sleep(3000); //Pause the game for a moment so user can read the score.
				
				i++;
			}
			
			//After the game is over, receive and print the final scores, and close I/O streams.
			System.out.println(in.readLine() + "\n");
			System.out.println("Wait for other players to finish for final scores...\n");
			System.out.println(in.readLine());
			System.out.println(in.readLine());
			System.out.println(in.readLine());
			System.out.println(in.readLine());
			System.out.println(in.readLine());
			
			in.close();
			out.close();
			input.close();
		}

		catch (UnknownHostException e) {
			System.err.println("Don't know about host");
			System.exit(1);
		}

		catch (IOException e) {
			System.err.println("Couldn't get I/O for connection to server");
			System.exit(1);
		} 

		catch (InterruptedException e) {
			System.err.println("Thread interrupted");
			e.printStackTrace();
		}
	}
}
