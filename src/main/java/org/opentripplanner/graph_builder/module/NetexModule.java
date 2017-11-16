package org.opentripplanner.graph_builder.module;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.io.InStream;
import org.apache.commons.io.IOUtils;
import org.opentripplanner.calendar.impl.MultiCalendarServiceImpl;
import org.opentripplanner.graph_builder.model.NetexBundle;
import org.opentripplanner.graph_builder.model.NetexDao;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.netex.mapping.AgencyAndIdFactory;
import org.opentripplanner.netex.mapping.NetexMapper;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.edgetype.factory.GtfsStopContext;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.rutebanken.netex.model.*;
import org.rutebanken.netex.model.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class NetexModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(NetexModule.class);

    private List<NetexBundle> netexBundles;

    private FareServiceFactory _fareServiceFactory = new DefaultFareServiceFactory();

    public NetexModule(List<NetexBundle> netexBundles) {
        this.netexBundles = netexBundles;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        graph.clearTimeZone();
        MultiCalendarServiceImpl calendarService = new MultiCalendarServiceImpl();
        GtfsStopContext stopContext = new GtfsStopContext();

        try {
            for(NetexBundle netexBundle : netexBundles){
                NetexDao netexDao = loadBundle(netexBundle);

                AgencyAndIdFactory.setAgencyId(netexBundle.netexParameters.netexFeedId);

                NetexMapper otpMapper = new NetexMapper(new OtpTransitBuilder());
                OtpTransitBuilder daoBuilder = otpMapper.mapNetexToOtp(netexDao);

                calendarService.addData(daoBuilder);

                GTFSPatternHopFactory hf = new GTFSPatternHopFactory(
                        new GtfsFeedId.Builder().id(netexBundle.netexParameters.netexFeedId).build(),
                        daoBuilder.build(),
                        _fareServiceFactory,
                        netexBundle.getMaxStopToShapeSnapDistance(),
                        netexBundle.subwayAccessTime,
                        netexBundle.maxInterlineDistance);
                hf.setStopContext(stopContext);
                hf.run(graph);

                if (netexBundle.linkStopsToParentStations) {
                    hf.linkStopsToParentStations(graph);
                }
                if (netexBundle.parentStationTransfers) {
                    hf.createParentStationTransfers();
                }
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }

        CalendarServiceData data = calendarService.getData();
        graph.putService(CalendarServiceData.class, data);
        graph.updateTransitFeedValidity(data);

        graph.hasTransit = true;
        graph.calculateTransitCenter();
    }

    @Override
    public void checkInputs() {
        netexBundles.forEach(NetexBundle::checkInputs);
    }

    private NetexDao loadBundle(NetexBundle netexBundle) throws Exception {
        LOG.info("Loading bundle " + netexBundle.getFilename());
        NetexDao netexDao = new NetexDao();
        List<ZipEntry> entries = netexBundle.getFileEntriesInOrder();
        Unmarshaller unmarshaller = getUnmarshaller();

        netexBundle.withZipFile(zipFile -> {
            for(ZipEntry entry : entries){
                LOG.info("Loading file " + entry.getName());
                loadFile(entryAsBytes(zipFile, entry), unmarshaller, netexDao);
            }
        });
        return netexDao;
    }

    public Collection<OtpTransitBuilder> getOtpDao() throws Exception {
        Collection<OtpTransitBuilder> otpDaoList = new ArrayList<>();

        for(NetexBundle bundle : netexBundles) {
            NetexDao netexDao = loadBundle(bundle);

            NetexMapper otpMapper = new NetexMapper(new OtpTransitBuilder());
            otpDaoList.add(otpMapper.mapNetexToOtp(netexDao));
        }

        return otpDaoList;
    }

    private Unmarshaller getUnmarshaller() throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(PublicationDeliveryStructure.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return unmarshaller;
    }


    private byte[] entryAsBytes(ZipFile zipFile, ZipEntry entry) {
        try {
            return IOUtils.toByteArray(zipFile.getInputStream(entry));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void loadFile(byte[] bytesArray, Unmarshaller unmarshaller, NetexDao netexDao) {
        try {
            @SuppressWarnings("unchecked")
            JAXBElement<PublicationDeliveryStructure> jaxbElement = (JAXBElement<PublicationDeliveryStructure>) unmarshaller
                    .unmarshal(new ByteArrayInputStream(bytesArray));

            PublicationDeliveryStructure value = jaxbElement.getValue();
            List<JAXBElement<? extends Common_VersionFrameStructure>> compositeFrameOrCommonFrames = value.getDataObjects().getCompositeFrameOrCommonFrame();
            for(JAXBElement frame : compositeFrameOrCommonFrames){

                if(frame.getValue() instanceof CompositeFrame) {
                    CompositeFrame cf = (CompositeFrame) frame.getValue();
                    VersionFrameDefaultsStructure frameDefaults = cf.getFrameDefaults();
                    String timeZone = "GMT";
                    if(frameDefaults != null && frameDefaults.getDefaultLocale() != null
                            && frameDefaults.getDefaultLocale().getTimeZone() != null){
                        timeZone = frameDefaults.getDefaultLocale().getTimeZone();
                    }

                    netexDao.setTimeZone(timeZone);
                    List<JAXBElement<? extends Common_VersionFrameStructure>> commonFrames = cf.getFrames().getCommonFrame();
                    for (JAXBElement commonFrame : commonFrames) {
                        loadResourceFrames(commonFrame, netexDao);
                        loadServiceCalendarFrames(commonFrame, netexDao);
                        loadTimeTableFrames(commonFrame, netexDao);
                        loadServiceFrames(commonFrame, netexDao);
                    }
                }
                else if (frame.getValue() instanceof SiteFrame) {
                    loadSiteFrames(frame, netexDao);
                }
            }
        } catch (JAXBException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // Stop places and quays
    private void loadSiteFrames(JAXBElement commonFrame, NetexDao netexDao) {
        if (commonFrame.getValue() instanceof SiteFrame) {
            SiteFrame sf = (SiteFrame) commonFrame.getValue();
            StopPlacesInFrame_RelStructure stopPlaces = sf.getStopPlaces();
            List<StopPlace> stopPlaceList = stopPlaces.getStopPlace();
            for (StopPlace stopPlace : stopPlaceList) {
                netexDao.getStopsById().put(stopPlace.getId(), stopPlace);
                if (stopPlace.getQuays() != null) {
                    List<Object> quayRefOrQuay = stopPlace.getQuays().getQuayRefOrQuay();
                    for (Object quayObject : quayRefOrQuay) {
                        if (quayObject instanceof Quay) {
                            Quay quay = (Quay) quayObject;
                            netexDao.getQuayById().put(quay.getId(), quay);
                            netexDao.getStopPlaceByQuay().put(quay, stopPlace);
                        }
                    }
                }
            }
        }
    }

    private void loadServiceFrames(JAXBElement commonFrame, NetexDao netexDao) {
        if (commonFrame.getValue() instanceof ServiceFrame) {
            ServiceFrame sf = (ServiceFrame) commonFrame.getValue();

            //stop assignments
            StopAssignmentsInFrame_RelStructure stopAssignments = sf.getStopAssignments();
            if(stopAssignments != null){
                List<JAXBElement<? extends StopAssignment_VersionStructure>> assignments = stopAssignments.getStopAssignment();
                for (JAXBElement assignment : assignments) {
                    if(assignment.getValue() instanceof PassengerStopAssignment) {
                        PassengerStopAssignment passengerStopAssignment = (PassengerStopAssignment) assignment.getValue();
                        if (passengerStopAssignment.getQuayRef() != null) {
                            if (netexDao.getQuayById().containsKey(passengerStopAssignment.getQuayRef().getRef())) {
                                Quay quay = netexDao.getQuayById().get(passengerStopAssignment.getQuayRef().getRef());
                                StopPlace stopPlace = netexDao.getStopPlaceByQuay().get(quay);
                                netexDao.getStopPointStopPlaceMap().put(passengerStopAssignment.getScheduledStopPointRef().getValue().getRef(), stopPlace.getId());
                                netexDao.getStopPointQuayMap().put(passengerStopAssignment.getScheduledStopPointRef().getValue().getRef(), quay.getId());
                            } else {
                                LOG.warn("Quay " + passengerStopAssignment.getQuayRef().getRef() + " not found in stop place file.");
                            }
                        }
                    }
                }
            }

            //routes
            RoutesInFrame_RelStructure routes = sf.getRoutes();
            if(routes != null){
                List<JAXBElement<? extends LinkSequence_VersionStructure>> route_ = routes.getRoute_();
                for (JAXBElement element : route_) {
                    if (element.getValue() instanceof Route) {
                        Route route = (Route) element.getValue();
                        netexDao.getRouteById().put(route.getId(), route);
                    }
                }
            }

            //network
            Network network = sf.getNetwork();
            if(network != null){
                OrganisationRefStructure orgRef = network.getTransportOrganisationRef().getValue();
                netexDao.getAuthoritiesByNetworkId().put(network.getId(), orgRef.getRef());
                if (network.getGroupsOfLines() != null) {
                    GroupsOfLinesInFrame_RelStructure groupsOfLines = network.getGroupsOfLines();
                    List<GroupOfLines> groupOfLines = groupsOfLines.getGroupOfLines();
                    for (GroupOfLines group : groupOfLines) {
                        netexDao.getAuthoritiesByGroupOfLinesId().put(group.getId(), orgRef.getRef());
                    }
                }
            }


            //lines
            LinesInFrame_RelStructure lines = sf.getLines();
            if(lines != null){
                List<JAXBElement<? extends DataManagedObjectStructure>> line_ = lines.getLine_();
                for (JAXBElement element : line_) {
                    if (element.getValue() instanceof Line) {
                        Line line = (Line) element.getValue();
                        netexDao.getLineById().put(line.getId(), line);
                    }

                }
            }

            //journeyPatterns
            JourneyPatternsInFrame_RelStructure journeyPatterns = sf.getJourneyPatterns();
            if(journeyPatterns != null){
                List<JAXBElement<?>> journeyPattern_orJourneyPatternView = journeyPatterns.getJourneyPattern_OrJourneyPatternView();
                for (JAXBElement pattern : journeyPattern_orJourneyPatternView) {
                    if (pattern.getValue() instanceof JourneyPattern) {
                        JourneyPattern journeyPattern = (JourneyPattern) pattern.getValue();
                        netexDao.getJourneyPatternsById().put(journeyPattern.getId(), journeyPattern);
                    }
                }
            }
        }
    }

    // ServiceJourneys
    private void loadTimeTableFrames(JAXBElement commonFrame, NetexDao netexDao) {
        if(commonFrame.getValue() instanceof TimetableFrame){
            TimetableFrame timetableFrame = (TimetableFrame) commonFrame.getValue();

            JourneysInFrame_RelStructure vehicleJourneys = timetableFrame.getVehicleJourneys();
            List<Journey_VersionStructure> datedServiceJourneyOrDeadRunOrServiceJourney = vehicleJourneys.getDatedServiceJourneyOrDeadRunOrServiceJourney();
            for(Journey_VersionStructure jStructure : datedServiceJourneyOrDeadRunOrServiceJourney){
                if(jStructure instanceof ServiceJourney){
                    loadServiceIds((ServiceJourney)jStructure, netexDao);
                    ServiceJourney sj = (ServiceJourney) jStructure;
                    String journeyPatternId = sj.getJourneyPatternRef().getValue().getRef();
                    if (netexDao.getJourneyPatternsById().containsKey(journeyPatternId)) {
                        if (netexDao.getJourneyPatternsById().get(journeyPatternId).getPointsInSequence().
                                getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().size()
                                == sj.getPassingTimes().getTimetabledPassingTime().size()) {

                            if (netexDao.getServiceJourneyById().get(journeyPatternId) != null) {
                                netexDao.getServiceJourneyById().get(journeyPatternId).add(sj);
                            } else {
                                netexDao.getServiceJourneyById().put(journeyPatternId, Lists.newArrayList(sj));
                            }
                        } else {
                            LOG.warn("Mismatch between ServiceJourney and JourneyPattern. ServiceJourney will be skipped. - " + sj.getId());
                        }
                    }
                    else {
                        LOG.warn("JourneyPattern not found. " + journeyPatternId);
                    }
                }
            }
        }
    }

    private void loadServiceIds (ServiceJourney serviceJourney, NetexDao netexDao) {
        DayTypeRefs_RelStructure dayTypes = serviceJourney.getDayTypes();
        StringBuilder serviceId = new StringBuilder();
        boolean first = true;
        for(JAXBElement dt : dayTypes.getDayTypeRef()){
            if(!first){
                serviceId.append("+");
            }
            first = false;
            if(dt.getValue() instanceof DayTypeRefStructure){
                DayTypeRefStructure dayType = (DayTypeRefStructure) dt.getValue();
                serviceId.append(dayType.getRef());
            }
        }

        // Add all unique service ids to map. Used when mapping calendars later.
        if (!netexDao.getServiceIds().containsKey(serviceId.toString())) {
            netexDao.getServiceIds().put(serviceId.toString(), serviceId.toString());
        }
    }

    // ServiceCalendar
    private void loadServiceCalendarFrames(JAXBElement commonFrame, NetexDao netexDao) {
        if (commonFrame.getValue() instanceof ServiceCalendarFrame){
            ServiceCalendarFrame scf = (ServiceCalendarFrame) commonFrame.getValue();

            if (scf.getServiceCalendar() != null) {
                DayTypes_RelStructure dayTypes = scf.getServiceCalendar().getDayTypes();
                for (JAXBElement dt : dayTypes.getDayTypeRefOrDayType_()) {
                    if (dt.getValue() instanceof DayType) {
                        DayType dayType = (DayType) dt.getValue();
                        netexDao.getDayTypeById().put(dayType.getId(), dayType);
                    }
                }
            }

            if (scf.getDayTypes() != null) {
                List<JAXBElement<? extends DataManagedObjectStructure>> dayTypes = scf.getDayTypes().getDayType_();
                for (JAXBElement dt : dayTypes) {
                    if (dt.getValue() instanceof DayType) {
                        DayType dayType = (DayType) dt.getValue();
                        netexDao.getDayTypeById().put(dayType.getId(), dayType);
                    }
                }
            }

            if (scf.getOperatingPeriods() != null) {
                for (OperatingPeriod_VersionStructure operatingPeriodStruct : scf.getOperatingPeriods().getOperatingPeriodOrUicOperatingPeriod()) {
                    OperatingPeriod operatingPeriod = (OperatingPeriod) operatingPeriodStruct;
                    netexDao.getOperatingPeriodById().put(operatingPeriod.getId(), operatingPeriod);
                }
            }

            List<DayTypeAssignment> dayTypeAssignments = scf.getDayTypeAssignments().getDayTypeAssignment();
            for(DayTypeAssignment dayTypeAssignment : dayTypeAssignments){
                String ref = dayTypeAssignment.getDayTypeRef().getValue().getRef();
                netexDao.getDayTypeAvailable().put(dayTypeAssignment.getId(), dayTypeAssignment.isIsAvailable() == null ? true : dayTypeAssignment.isIsAvailable());

                if (netexDao.getDayTypeAssignment().containsKey(ref)) {
                    netexDao.getDayTypeAssignment().get(ref).add(dayTypeAssignment);
                } else {
                    netexDao.getDayTypeAssignment().put(ref, new ArrayList<DayTypeAssignment>() {
                        {
                            add(dayTypeAssignment);
                        }
                    });
                }
            }
        }
    }

    // Authorities and operators
    private void loadResourceFrames(JAXBElement commonFrame, NetexDao netexDao) {
        if(commonFrame.getValue() instanceof ResourceFrame){
            ResourceFrame resourceFrame = (ResourceFrame) commonFrame.getValue();
            List<JAXBElement<? extends DataManagedObjectStructure>> organisations = resourceFrame.getOrganisations().getOrganisation_();
            for(JAXBElement element : organisations){
                if(element.getValue() instanceof Authority){
                    Authority authority = (Authority) element.getValue();
                    netexDao.getAuthorities().put(authority.getId(), authority);
                }
                if(element.getValue() instanceof Operator){
                    Operator operator = (Operator) element.getValue();
                    netexDao.getOperators().put(operator.getId(), operator);
                }
            }
        }
    }
}