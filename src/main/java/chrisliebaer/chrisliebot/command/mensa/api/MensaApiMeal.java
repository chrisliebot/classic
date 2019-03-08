package chrisliebaer.chrisliebot.command.mensa.api;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * Class representation of JSON object describing meals.
 */
@ToString
public class MensaApiMeal {
	
	@Getter private String meal;
	@Getter private String dish;
	
	@Getter private List<String> add;
	@Getter private boolean bio;
	@Getter private boolean fish;
	@Getter private boolean pork;
	
	@Getter @SerializedName("pork_raw") private boolean porkRaw;
	@Getter private boolean cow;
	@Getter @SerializedName("cow_aw") private boolean cowRaw;
	@Getter private boolean vegan;
	@Getter private boolean veg;
	@Getter @SerializedName("mensa_vit") private boolean mensaVit;
	
	@Getter private String info;
	
	@Getter @SerializedName("price_1") private BigDecimal price1; // students
	@Getter @SerializedName("price_2") private BigDecimal price2; // guests
	@Getter @SerializedName("price_3") private BigDecimal price3; // employees
	@Getter @SerializedName("price_4") private BigDecimal price4; // pupils
	
	@Getter @SerializedName("price_flag") private int priceFlag;
	
	// API handles empty lines in a very strange way, so we just roll with it
	@Getter @Deprecated @SerializedName("nodata") private boolean noData;
	@Getter @Deprecated @SerializedName("closing_start") private long closedFrom;
	@Getter @Deprecated @SerializedName("closing_end") private long closedTo;
	@Getter @Deprecated @SerializedName("closing_text") private String closingText;
}
