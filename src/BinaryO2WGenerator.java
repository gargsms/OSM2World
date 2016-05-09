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
import org.osm2world.core.world.modules.TreeModule;
import org.osm2world.core.world.modules.RoadModule;
import org.osm2world.core.world.modules.RailwayModule;
import org.osm2world.core.world.modules.SurfaceAreaModule;
import org.osm2world.core.world.modules.WaterModule;

import com.google.common.io.LittleEndianDataOutputStream;


public class BinaryO2WGenerator {

	public static void main(String[] args) throws IOException {

		OutputStream fileOutputStream = new FileOutputStream(new File(args[1]));
		DataOutputStream outputStream = new DataOutputStream(fileOutputStream);
		LittleEndianDataOutputStream leOutputStream = new LittleEndianDataOutputStream(outputStream);
		CustomBinaryTarget target = new CustomBinaryTarget(leOutputStream);

		ConversionFacade facade = new ConversionFacade();

		Configuration config = new BaseConfiguration();
		config.addProperty("createTerrain", "true");

		List<WorldModule> worldModules = new ArrayList<>();
		worldModules.add(new BuildingModule());
		worldModules.add(new TreeModule());
		worldModules.add(new RailwayModule());
		worldModules.add(new RoadModule());
		worldModules.add(new SurfaceAreaModule());
		worldModules.add(new WaterModule());

		facade.createRepresentations(new File(args[0]),
				worldModules, config, Arrays.<Target<?>>asList(target));

		leOutputStream.close();
		outputStream.close();
		fileOutputStream.close();

	}

}
