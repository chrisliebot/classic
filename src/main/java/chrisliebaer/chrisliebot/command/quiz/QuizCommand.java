package chrisliebaer.chrisliebot.command.quiz;

import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;

public class QuizCommand implements CommandExecutor {
	
	/* Entwurf für Command Interface und Verhalten
	 *
	 * !quiz <stop|SETNAME|list>
	 * fragen können aus verschiedenen interface implementierungen kommen
	 * fragentypen: multiple choice, freitext, levensthein distance
	 */
	
	@Override
	public void execute(Message m, String arg) {
	
	}
}
