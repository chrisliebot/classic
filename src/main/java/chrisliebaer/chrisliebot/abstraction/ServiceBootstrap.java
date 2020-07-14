package chrisliebaer.chrisliebot.abstraction;

import chrisliebaer.chrisliebot.Chrisliebot;

public interface ServiceBootstrap {
	
	/**
	 * Called during startup to create a service from the provided config. Service startup is done asynchronously.
	 *
	 * @param chrisliebot A Chrisliebot instance that will power the instanced service.
	 * @param identifier  The identifier of the to be created service.
	 * @return A configured and fully functional {@link ChrislieService}.
	 */
	public ChrislieService service(Chrisliebot chrisliebot, String identifier) throws Exception;
}
