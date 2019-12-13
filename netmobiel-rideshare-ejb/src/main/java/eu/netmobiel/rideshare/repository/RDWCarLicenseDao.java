package eu.netmobiel.rideshare.repository;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;

import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.CarType;

@ApplicationScoped
public class RDWCarLicenseDao {
    @Inject
    private Logger log;

//  $$app_token=....
    @Resource(lookup = "java:global/licensePlate/RDWAppToken")
    private String rdwAppToken;

//  https://opendata.rdw.nl/resource/m9d7-ebf2.json?kenteken=52PHVD&$select=kenteken,voertuigsoort,merk,handelsbenaming,inrichting,aantal_zitplaatsen,eerste_kleur,tweede_kleur,aantal_deuren,aantal_wielen,datum_eerste_toelating,typegoedkeuringsnummer
    @Resource(lookup = "java:global/licensePlate/RDWVoertuigenUrl")
    private String rdwVoertuigenUrl;

//  https://opendata.rdw.nl/resource/8ys7-d773.json?kenteken=52PHVD&$select=co2_uitstoot_gecombineerd
    @Resource(lookup = "java:global/licensePlate/RDWBrandstofUrl")
    private String rdwBrandstofUrl; 

    @SuppressWarnings("el-syntax")
    public Car fetchDutchLicensePlateInformation(String plate) throws IOException {
		plate = plate.toUpperCase();
		Map<String, String> valuesMap = new HashMap<>();
		valuesMap.put("APP_TOKEN", rdwAppToken);
		valuesMap.put("KENTEKEN", plate.replace("-", ""));
		StringSubstitutor substitutor = new StringSubstitutor(valuesMap, "#{", "}");
		String url = substitutor.replace(rdwVoertuigenUrl);
		Client client = ClientBuilder.newBuilder().build();
		WebTarget target = client.target(url);
		String result = null;
		log.debug(String.format("RDW voertuig for %s", plate));
		Car car = null;
		try (Response response = target.request().get()) {
	        result = response.readEntity(String.class);
	        car = convertRDW2Car(plate, result);
		} catch (IOException e) {
			log.error("Unable to parse RDW license JSON - " + e.toString());
			throw e;
		}
		if (car != null) {
			url = substitutor.replace(rdwBrandstofUrl);
			target = client.target(url);
			try (Response response = target.request().get()) {
		        result = response.readEntity(String.class);
		        convertRDWFuel2Car(car, plate, result);
			} catch (IOException e) {
				log.error("Unable to parse RDW fuel JSON - " + e.toString());
				throw e;
			}
		}
        return car;
    }

    private Car convertRDW2Car(String plate, String json) throws IOException {
    	JsonArray licenseList = Json.createReader(new StringReader(json)).readArray();
    	if (licenseList.size() == 0) { 
            return null;
        } else if (licenseList.size() > 1) {
        	log.warn(String.format("Multiple results for plate %s", plate));
        }
        JsonObject node = licenseList.get(0).asJsonObject();
        Car car = new Car();
		car.setLicensePlate(plate);
        car.setBrand(node.getString("merk", null));
        car.setModel(node.getString("handelsbenaming", null));
        car.setColor(node.getString("eerste_kleur", null));
        String color2 = node.getString("tweede_kleur", null);
        if (!color2.equalsIgnoreCase("Niet geregistreerd")) {
        	car.setColor2(color2);
        }
        String toelating = node.getString("datum_eerste_toelating", null);
        if (toelating != null && toelating.length() >= 4) {
        	String year = node.getString("datum_eerste_toelating").substring(0, 4);
	        if (StringUtils.isNumeric(year)) {
	            car.setRegistrationYear(Integer.parseInt(year));
	        }
        }
        car.setNrSeats(getStringAsInt(node, "aantal_zitplaatsen"));
        car.setNrDoors(getStringAsInt(node, "aantal_deuren"));
        String ctype = node.getString("inrichting", null);
        if (!ctype.equalsIgnoreCase("Niet geregistreerd")) {
	        car.setType(carTypeMapping.get(ctype));
	        if (car.getType() == null) {
	        	car.setType(CarType.OTHER);
	        }
        }
//      String soort = node.get("voertuigsoort").asText();
        car.setTypeRegistrationId(node.getString("typegoedkeuringsnummer", null));
        return car;
    }
    
    private Integer getStringAsInt(JsonObject node, String field) {
    	String s = node.getString(field, null);
    	return StringUtils.isNumeric(s) ? Integer.parseInt(s) : null;
    }
    
    private void convertRDWFuel2Car(Car car, String plate, String json) throws IOException {
    	JsonArray cars = Json.createReader(new StringReader(json)).readArray();
    	if (cars.size() == 0) { 
            return;
        } else if (cars.size() > 1) {
        	log.warn(String.format("Multiple fuel results for plate %s", plate));
        }
        JsonObject node = cars.get(0).asJsonObject();
        car.setCo2Emission(getStringAsInt(node, "co2_uitstoot_gecombineerd"));
    }

    private static final Map<String, CarType> carTypeMapping = new LinkedHashMap<>();
    static {
    	carTypeMapping.put("cabriolet", CarType.CONVERTIBLE);
    	carTypeMapping.put("coupe", CarType.COUPE);
    	carTypeMapping.put("hatchback", CarType.HATCHBACK);
    	carTypeMapping.put("MPV", CarType.SUV);
    	carTypeMapping.put("sedan", CarType.SALOON);
    	carTypeMapping.put("stationwagen", CarType.ESTATE);
    }
    
// Alle voertuigsoorten
//    Aanhangwagen
//    Autonome aanhangwagen
//    Bedrijfsauto
//    Bromfiets
//    Bus
//    Driewielig motorrijtuig
//    Middenasaanhangwagen
//    Motorfiets
//    Motorfiets met zijspan
//    Oplegger
//    Personenauto 
    
// Alle inrichtingen uit RDW database    
//    aanhangwagentrekker
//    aanhangw. Met stijve dissel
//    achterwaartse kipper
//    afneembare bovenbouw
//    afzetbak
//    ambulance
//    bergingsvoertuig
//    betonmixer
//    betonpomp
//    boorwagen
//    brandweerwagen
// +   cabriolet
//    caravan
//    chassis cabine
//    compressor
//    containercarrier
// +   coupe
//    demonstratiewagen
//    detailhandel/expositiedoel.
//    dieplader
//    dolly
//    driewielig motorrijtuig (L5e)
//    driewielig motorrijtuig (L7e)
//    driezijdige kipper
//    faecalienwagen
//    geconditioneerd voertuig
//    gecond. met temperatuurreg.
//    gecond. zndr temperatuurreg.
//    gedeeltelijk open wagen
//    geluidswagen
//    gepantserd voertuig
// +   hatchback
//    hoogwerker
//    huifopbouw
//    kaal chassis
//    kampeerwagen
//    kantoorwagen
//    keetwagen
//    kipper
//    kolkenzuiger
//    kraanwagen
//    lijkwagen
//    limousine
//    medische hulpwagen
//    meetwagen
//    mobiele kraan
//    mobiele zender
//    montagewagen
// +   MPV
//    neerklapbare zijschotten
// +   Niet geregistreerd
// +   niet nader aangeduid
// +   N.v.t.
//    open laadvloer
//    open met kraan
//    open wagen
//    open wagen met vast dak
//    opleggertrekker
//    pick-up truck
//    reparatiewagen
//    resteelwagen
// +   sedan
//    servicewagen
//    speciale groep
//    sproeiwagen
// +   stationwagen
//    straatveegwagen
//    straatvgr,reiniger,rioolzgr
//    takelwagen
//    tank v.v. gevaarl. Stoffen
//    taxi
// +   terreinvoertuig
//    truckstationwagen
//    tweezijdige kipper
//    vervoer van uitzond. Lading
//    voertuig met haakarm
//    voor rolstoelen toegankelijk voertuig
//    voor vervoer boomstammen
//    voor vervoer boten
//    voor vervoer personen
//    voor vervoer voertuigen
//    voor vervoer wissellaadbakken
//    vrieswagen
//    vuilniswagen
//    v.vervoer zweefvliegtuigen
//    woonwagen
    

}
