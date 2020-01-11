package chrisliebaer.chrisliebot.command.quiz;

import chrisliebaer.chrisliebot.command.ChrislieListener;

public class QuizCommand implements ChrislieListener.Command {
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		throw new RuntimeException("not implemented"); //TODO
	}
	
	/* Entwurf für Command Interface und Verhalten
	 *
	 * !quiz <stop|SETNAME|list>
	 * fragen können aus verschiedenen interface implementierungen kommen
	 * fragentypen: multiple choice, freitext, levensthein distance
	 */
}
