package chrisliebaer.chrisliebot.abstraction;

public interface PlainOutput {
	
	public PlainOutput append(String s, Object... format);
	
	public PlainOutput appendEscape(String s, Object... format);
	
	public PlainOutput newLine();
	
	public PlainOutput clear();
	
	public interface PlainOuputSubstitution extends PlainOutput {
		
		public PlainOuputSubstitution appendSub(String s, Object... format);
		
		public PlainOuputSubstitution appendEscapeSub(String s, Object... format);
		
		@Override
		public PlainOuputSubstitution append(String s, Object... format);
		
		@Override
		public PlainOuputSubstitution appendEscape(String s, Object... format);
		
		@Override
		public PlainOuputSubstitution newLine();
		
		@Override
		public PlainOuputSubstitution clear();
		
		public static String escape(String in) {
			throw new RuntimeException("not yet implemented");
		}
	}
}
