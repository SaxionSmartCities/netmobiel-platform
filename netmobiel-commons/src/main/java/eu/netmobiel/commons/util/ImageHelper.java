package eu.netmobiel.commons.util;

import java.util.Arrays;
import java.util.Base64;

import javax.ws.rs.BadRequestException;

public class ImageHelper {
	public static class DecodedImage {
		public String filetype;
		public String mimetype;
		public byte[] decodedImage;
	}
	
	private ImageHelper() {
		// Only static methods
	}
	
	public static DecodedImage decodeImage(String data, String[] allowedFiletypes) {
		DecodedImage di = new DecodedImage();
		// See https://developer.mozilla.org/en-US/docs/Web/API/FileReader/readAsDataURL
		// This is how the client passes the image
		// Format: data:image/*;base64,
		final String prefix = "data:";
		if (data == null || ! data.startsWith(prefix)) {
			throw new BadRequestException("Uploaded image data must be a data url");
		}
		String[] parts = data.substring(prefix.length()).split(",");
		String spec[] = parts[0].split(";");
		di.mimetype = spec[0];
		if (!di.mimetype.startsWith("image/")) {
			throw new BadRequestException("Uploaded file does not have an image mimetype: " + di.mimetype);
		}
		di.filetype = di.mimetype.substring(di.mimetype.indexOf("/") + 1);
		Arrays.stream(allowedFiletypes)
			.filter(type -> type.equals(di.filetype))
			.findFirst()
			.orElseThrow(() -> new BadRequestException(
					String.format("Uploaded image type %s not supported, use one of [%s]", 
							di.filetype, String.join(", ", allowedFiletypes))
					));
		String encoding = spec.length > 1 ? spec[1] : null;
		if (encoding == null || !encoding.equals("base64")) {
			throw new BadRequestException("Uploaded image encoding not supported: " + encoding);
		}
		di.decodedImage = Base64.getDecoder().decode(parts[1]);
		return di;
	}

}
