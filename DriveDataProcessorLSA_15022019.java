package de.gsi.csco.ap.app_drivestat.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cern.accsoft.commons.domain.particletransfers.ParticleTransfer;
import cern.accsoft.commons.util.Nameds;
import cern.lsa.client.DeviceService;
import cern.lsa.client.ParameterService;
import cern.lsa.client.Services;
import cern.lsa.domain.devices.Device;
import cern.lsa.domain.devices.DeviceMetaTypeEnum;
import cern.lsa.domain.devices.factory.DevicesRequestBuilder;
import cern.lsa.domain.settings.BeamProductionChain;
import cern.lsa.domain.settings.Contexts;
import cern.lsa.domain.settings.DrivableBeamProcess;
import cern.lsa.domain.settings.Parameter;
import cern.lsa.domain.settings.ParameterType;
import cern.lsa.domain.settings.Parameters;
import cern.lsa.domain.settings.Pattern;
import cern.lsa.domain.settings.factory.ParameterTypesRequestBuilder;
import cern.lsa.domain.settings.factory.ParametersRequestBuilder;
import cern.lsa.domain.settings.type.BeamProcessTypeCategory;
import de.gsi.cs.co.lsa.domain.particletransfers.Gsi_hebtParticleTransfer;

// for library loggers
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// for application loggers
// import de.gsi.cs.co.ap.common.gui.elements.logger.AppLogger;

/**
 * @author fschirru
 */
public class DriveDataProcessorLSA {

    // You can choose a logger (needed imports are given in the import section as comments):
    // for libraries:
    // private static final Logger LOGGER = LoggerFactory.getLogger(DriveDataProcessorLSA.class);
    // for applications:
    // private static final AppLogger LOGGER = AppLogger.getLogger();

    private static final ParticleTransfer[] FRS_PARTICLE_TRANSFERS = new ParticleTransfer[] {
            Gsi_hebtParticleTransfer.SIS18_TO_GTS1MU1, Gsi_hebtParticleTransfer.GTS1MU1_TO_GTS3MU1,
            Gsi_hebtParticleTransfer.GTS3MU1_TO_GHFSMU1, Gsi_hebtParticleTransfer.GHFSMU1_TO_GTS6MU1,
            Gsi_hebtParticleTransfer.GTS6MU1_TO_GTS6MU1, Gsi_hebtParticleTransfer.GTS6MU1_TO_GTS7MU1,
            Gsi_hebtParticleTransfer.GTS7MU1_TO_GTH4MU1, Gsi_hebtParticleTransfer.GTH4MU1_TO_GTH4MU2,
            Gsi_hebtParticleTransfer.GTH4MU2_TO_GTV1MU1, Gsi_hebtParticleTransfer.GTV1MU1_TO_GHTDMU1,
            Gsi_hebtParticleTransfer.GHTDMU1_TO_HTC, Gsi_hebtParticleTransfer.GHFSMU1_TO_HFS,
            Gsi_hebtParticleTransfer.GTS6MU1_TO_ESR };

    private static final String[] PARAMETER_TYPES = new String[] { "SCALAR_TARGET_XPOS", "SCALAR_SLIT_OPOS",
            "SCALAR_SLIT_UPOS", "SCALAR_SLIT_LPOS", "SCALAR_SLIT_RPOS" };

    private static final String LSA_PREFIX_LOGICAL_DEVICES = "LOGICAL.";

    private static final DriveDataProcessorLSA INSTANCE = new DriveDataProcessorLSA();

    List<Object[]> driveDataList = new ArrayList<>(); // Data container

    private final DeviceService deviceService = Services.getDeviceService();
    private final ParameterService parameterService = Services.getParameterService();

    private final Set<Device> myDevices = new HashSet<>(); // The list containing all the DS and PLA devices
    private Set<Device> myDSdevices = new HashSet<>(); // The list containing the DS devices
    private Set<Device> myPLAdevices = new HashSet<>(); // The list containing the PLA devices

    private final List<Device> myFrsSortedDevices = new ArrayList<>();

    private BeamProductionChain chain;
    private Pattern pattern;

    private final Set<Device> filteredFrsDeviceNames = new HashSet<>();
    private final Set<Parameter> staticParameterList = new HashSet<>();

    // private AcceleratorZone deviceAcceleratorZone;

    DriveDataProcessorLSA() {

    }

    public static DriveDataProcessorLSA getInstance() {

        return INSTANCE;
    }

    public void setPattern(final Pattern pattern) {

        this.pattern = pattern;
    }

    public void setBeamChain(final BeamProductionChain chain) {

        this.chain = chain;
    }

    public void setStaticDevices() {

        // Find static list of FRS devices
        final DevicesRequestBuilder devicesRequestBuilder_DS = new DevicesRequestBuilder();
        final DevicesRequestBuilder devicesRequestBuilder_PLA = new DevicesRequestBuilder();
        final Set<ParticleTransfer> frsParticleTransfers = new HashSet<>();

        Collections.addAll(frsParticleTransfers, FRS_PARTICLE_TRANSFERS);
        devicesRequestBuilder_DS.setParticleTransfers(frsParticleTransfers);
        devicesRequestBuilder_DS.setMetaType(DeviceMetaTypeEnum.ACTUAL);
        devicesRequestBuilder_DS.setDeviceTypeName("DS"); // StepperMotor Drive

        devicesRequestBuilder_PLA.setParticleTransfers(frsParticleTransfers);
        devicesRequestBuilder_PLA.setMetaType(DeviceMetaTypeEnum.ACTUAL);
        devicesRequestBuilder_PLA.setDeviceTypeName("PLA"); // Pneumatic Drives

        myDSdevices = deviceService.findDevices(devicesRequestBuilder_DS.build());
        myPLAdevices = deviceService.findDevices(devicesRequestBuilder_PLA.build());

        myDevices.addAll(myDSdevices);
        myDevices.addAll(myPLAdevices);

        System.out.println("*** THE FULL LIST OF FRS DEVICES ***");
        for (final Device device : myDevices) {

            System.out.println(device.getName() + " , " + device.getDeviceType().getName() + " , "
                    + device.getPosition() + " , " + device.getSortOrder());

        }

        System.out.println("Number of FRS devices " + myDevices.size());

        myFrsSortedDevices.clear();

        // Sort (position wise) the full list of devices. Later the list will be filtered out for the selected Chain
        for (final Device device : myDevices) {

            myFrsSortedDevices.add(device);
        }

        final DeviceComparator dc = new DeviceComparator();
        myFrsSortedDevices.sort(dc);

        for (int m = 0; m < myFrsSortedDevices.size(); m++) {

            System.out.println(myFrsSortedDevices.get(m).getName() + " , "
                    + myFrsSortedDevices.get(m).getDeviceType().getName() + " , "
                    + myFrsSortedDevices.get(m).getPosition() + " , " + myFrsSortedDevices.get(m).getSortOrder()
                    + myFrsSortedDevices.get(m).getAcceleratorZone().getParticleTransfers().toString() + " *** "
                    + myFrsSortedDevices.get(m).getDescription());

        }

        System.out.println("Number of FRS sorted devices " + myFrsSortedDevices.size());

        getDevicesData();
    }

    // public void getDevicesData(final Set<Device> myDevices) {
    public void getDevicesData() {

        // Clear list to avoid summing up of items while changing the chain
        // System.out.println(parameterTypesRequestBuilder.);
        driveDataList.clear();

        // Look for parameter types that we are interested in
        final ParameterTypesRequestBuilder parameterTypesRequestBuilder = new ParameterTypesRequestBuilder();
        parameterTypesRequestBuilder.setParameterTypeNames(Arrays.asList(PARAMETER_TYPES));
        final Set<ParameterType> parameterTypes = parameterService
                .findParameterTypes(parameterTypesRequestBuilder.build());
        System.out.println("*** LIST OF PARAMETER TYPE ***");
        for (final ParameterType parameterType : parameterTypes) {
            System.out.println(parameterType);
        }

        final ParametersRequestBuilder parametersRequestBuilder = new ParametersRequestBuilder();
        final Set<String> lsaDeviceNames = new HashSet<>();

        if (!myDevices.isEmpty()) {

            filteredFrsDeviceNames.clear();
            staticParameterList.clear();

            for (final Device device : myDevices) {

                // System.out.println("XX--XX");
                // System.out.println(device.getName());
                // for (final Device device : frsDevices) {
                lsaDeviceNames.add(LSA_PREFIX_LOGICAL_DEVICES + device.getName());
                // System.out.println(LSA_PREFIX_LOGICAL_DEVICES + device.getName());

                System.out.println(device.getName() + " , " + device.getDeviceType().getName() + " , "
                        + device.getPosition() + " , " + device.getSortOrder());

            }

            parametersRequestBuilder.setDeviceNames(lsaDeviceNames);
            parametersRequestBuilder.setParameterTypeNames(Nameds.getNames(parameterTypes));
            final Set<Parameter> parameters = parameterService.findParameters(parametersRequestBuilder.build());

            System.out.println("lsaDeviceNames " + lsaDeviceNames.size());
            System.out.println("*** THE LIST OF PARAMETERS ***");
            for (final Parameter parameter : parameters) {
                staticParameterList.add(parameter);
                System.out.println(parameter);
                // System.out.println(parameter.getParameterType());

            }

            System.out.println("Number of Parameters " + parameters.size());

            pattern = (Pattern) chain.getParent();

            final List<DrivableBeamProcess> allChainBeamProcesses = chain.getDrivableBeamProcesses();
            final List<DrivableBeamProcess> beamInBeamProcesses = Contexts.filterBeamProcesses(allChainBeamProcesses,
                    null, BeamProcessTypeCategory.FUNCTION_BEAM_IN);

            System.out.println("*** beamInBeamProcesses ***");
            System.out.println(beamInBeamProcesses);
            System.out.println(beamInBeamProcesses.size());

            // final List<DrivableBeamProcess> beamInBeamProcesses = Contexts.filterBeamProcesses(allChainBeamProcesses,
            // Gsi_hebtParticleTransfer.GTS1MU1_TO_GTS3MU1, BeamProcessTypeCategory.FUNCTION_BEAM_IN);

            final Set<Parameter> parametersFilteredOnChain = Parameters.filterParametersByParticleTransfers(
                    staticParameterList, Contexts.getParticleTransfersFromBeamProcesses(beamInBeamProcesses));

            // parametersFilteredOnChain.add(par); // to do 4/4/2018

            System.out.println(parametersFilteredOnChain.size());

            System.out.println("*** THE LIST OF PARAMETERS FILTERED ON CHAIN ***");

            for (final Parameter parameter : parametersFilteredOnChain) {

                // System.out.println(parameter.getDevice().getPosition());
                filteredFrsDeviceNames.add(parameter.getDevice());
                System.out.println(parameter.getName());
            }

            // Clear list to avoid summing up of items while changing the chain
            driveDataList.clear();

            if (myFrsSortedDevices != null) {

                for (int i = 0; i < myFrsSortedDevices.size(); i++) {

                    driveDataList.add(new Object[] { myFrsSortedDevices.get(i).getName(),
                            myFrsSortedDevices.get(i).getDeviceType().getName(),
                            myFrsSortedDevices.get(i).getAcceleratorZone() });
                }
            }
        }
    }

    public Set<Device> getStaticDevices() {

        // return myDevices;
        // return frsDevices;
        return filteredFrsDeviceNames;
    }

    public List<Device> getDevices() {

        // return myDevices;
        // return frsDevices;
        return myFrsSortedDevices;
    }

    public List<Object[]> getDriveData() {

        return driveDataList;
    }

}
