package chrisliebaer.chrisliebot.command.haskell;


import lombok.Builder;
import lombok.Data;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface HaskellService {
	
	@Headers("Content-Type: application/json")
	@POST("rpc")
	public Call<Output> runHaskell(@Body Param param);
	
	@Data
	@Builder
	public static class Param {
		private String proc;
		private Args args;
	}
	
	@Data
	@Builder
	public static class Args {
		private String expression;
		private String timelimit;
	}
	
	@Data
	@Builder
	public static class Output {
		private int returncode;
		private String output;
	}
}
