package chrisliebaer.chrisliebot.abstraction;

public interface PlainOutput {
	
	public PlainOutput append(String s, Object... format);
	
	public PlainOutput appendEscape(String s, Object... format);
	
	public PlainOutput newLine();
	
	public PlainOutput clear();
	
	public interface PlainOutputSubstituion extends PlainOutput {
		
		public PlainOutputSubstituion appendSub(String s, Object... format);
		
		public PlainOutputSubstituion appendEscapeSub(String s, Object... format);
		
		@Override
		public PlainOutputSubstituion append(String s, Object... format);
		
		@Override
		public PlainOutputSubstituion appendEscape(String s, Object... format);
		
		@Override
		public PlainOutputSubstituion newLine();
		
		@Override
		public PlainOutputSubstituion clear();
		
		public static String escape(String in) {
			throw new RuntimeException("not yet implemented");
		}
	}
	
	public static PlainOutputSubstituion dummy() {
		return PlainOutputDummy.DUMMY;
	}
	
	public static final class PlainOutputDummy implements PlainOutputSubstituion {
		
		private static final PlainOutputDummy DUMMY = new PlainOutputDummy();
		
		private PlainOutputDummy() { }
		
		@Override
		public PlainOutputSubstituion appendSub(String s, Object... format) { return this; }
		
		@Override
		public PlainOutputSubstituion appendEscapeSub(String s, Object... format) { return this; }
		
		@Override
		public PlainOutputSubstituion append(String s, Object... format) { return this; }
		
		@Override
		public PlainOutputSubstituion appendEscape(String s, Object... format) { return this; }
		
		@Override
		public PlainOutputSubstituion newLine() { return this; }
		
		@Override
		public PlainOutputSubstituion clear() { return this; }
	}
}
