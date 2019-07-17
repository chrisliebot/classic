package chrisliebaer.chrisliebot.command.quiz;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;

public class QuizCommand implements ChrisieCommand {
	
	/* Entwurf für Command Interface und Verhalten
	 *
	 * !quiz <stop|SETNAME|list>
	 * fragen können aus verschiedenen interface implementierungen kommen
	 * fragentypen: multiple choice, freitext, levensthein distance
	 */
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
	
	}
}
