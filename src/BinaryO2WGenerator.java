import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.custom_binary.CustomBinaryTarget;
import org.osm2world.core.world.creation.WorldModule;
import org.osm2world.core.world.modules.BuildingModule;

import com.google.common.io.LittleEndianDataOutputStream;


public class BinaryO2WGenerator {

	public static void main(String[] args) throws IOException {

		OutputStream fileOutputStream = new FileOutputStream(new File(args[1]));
		DataOutputStream outputStream = new DataOutputStream(fileOutputStream);
		LittleEndianDataOutputStream leOutputStream = new LittleEndianDataOutputStream(outputStream);
		CustomBinaryTarget target = new CustomBinaryTarget(leOutputStream);

		ConversionFacade facade = new ConversionFacade();

		Configuration config = new BaseConfiguration();
		config.addProperty("createTerrain", "false");

		List<WorldModule> worldModules = new ArrayList<>();
		worldModules.add(new BuildingModule());

		facade.createRepresentations(new File(args[0]),
				worldModules, config, Arrays.<Target<?>>asList(target));

		leOutputStream.close();
		outputStream.close();
		fileOutputStream.close();

	}

}
