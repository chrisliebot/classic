package chrisliebaer.chrisliebot.util;

import chrisliebaer.chrisliebot.C;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import lombok.Getter;
import lombok.NonNull;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.annotation.Nullable;
import javax.validation.Validator;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.stream.Collectors;

public class GsonValidator {
	
	@Getter private final Gson gson;
	@Getter private final Validator validator;
	
	private final Converter.Factory factory;
	
	public GsonValidator(@NonNull Gson gson, @NonNull Validator validator) {
		this.gson = gson;
		this.validator = validator;
		
		factory = new ValidatingRetrofitFactory();
	}
	
	public Converter.Factory factory() {
		return factory;
	}
	
	public <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
		return validate(gson.fromJson(json, classOfT));
	}
	
	public <T> T fromJson(String json, Type typeOfT) throws JsonSyntaxException {
		return validate(gson.fromJson(json, typeOfT));
	}
	
	public <T> T fromJson(Reader json, Class<T> classOfT) throws JsonSyntaxException, JsonIOException {
		return validate(gson.fromJson(json, classOfT));
	}
	
	public <T> T fromJson(Reader json, Type typeOfT) throws JsonIOException, JsonSyntaxException {
		return validate(gson.fromJson(json, typeOfT));
	}
	
	public <T> T fromJson(JsonReader reader, Type typeOfT) throws JsonIOException, JsonSyntaxException {
		return validate(gson.fromJson(reader, typeOfT));
	}
	
	public <T> T fromJson(JsonElement json, Class<T> classOfT) throws JsonSyntaxException {
		return validate(gson.fromJson(json, classOfT));
	}
	
	public <T> T fromJson(JsonElement json, Type typeOfT) throws JsonSyntaxException {
		return validate(gson.fromJson(json, typeOfT));
	}
	
	public void toJson(Object description, FileWriter fw) throws JsonSyntaxException {
		gson.toJson(validate(description), fw);
	}
	
	private <T> T validate(T t) throws JsonSyntaxException {
		var result = validator.validate(t);
		if (!result.isEmpty()) {
			var reason = result.stream().map(v -> String.format("%s value `%s`: %s",
					v.getPropertyPath(), v.getInvalidValue(), v.getMessage()))
					.collect(Collectors.joining("\n"));
			throw new JsonSyntaxException("The given JSON object is invalid: \n" + reason);
		}
		return t;
	}
	
	private class ValidatingRetrofitFactory extends Converter.Factory {
		
		private GsonConverterFactory gsonFactory = GsonConverterFactory.create(gson);
		
		@Nullable
		@Override
		public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
			Converter<ResponseBody, ?> converter = gsonFactory.responseBodyConverter(type, annotations, retrofit);
			return body -> validate(converter.convert(body));
		}
		
		@Nullable
		@Override
		public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
			Converter<?, RequestBody> converter = gsonFactory.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit);
			return value -> validate(converter.convert(C.unsafeCast(value))); // type compatibility is upheld by retrofit
		}
	}
	
	private static class RequestConverterHelper<T> implements Converter<T, RequestBody> {
		
		@Nullable
		@Override
		public RequestBody convert(T value) throws IOException {
			return null;
		}
	}
}
