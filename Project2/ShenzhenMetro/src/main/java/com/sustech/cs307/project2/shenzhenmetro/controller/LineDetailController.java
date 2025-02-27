package com.sustech.cs307.project2.shenzhenmetro.controller;

import com.sustech.cs307.project2.shenzhenmetro.dto.LineDetailNavigateDto;
import com.sustech.cs307.project2.shenzhenmetro.object.LineDetail;
import com.sustech.cs307.project2.shenzhenmetro.dto.LineDetailDto;
import com.sustech.cs307.project2.shenzhenmetro.dto.LineDetailSearchDto;
import com.sustech.cs307.project2.shenzhenmetro.repository.LineDetailRepository;
import com.sustech.cs307.project2.shenzhenmetro.repository.LineRepository;
import com.sustech.cs307.project2.shenzhenmetro.repository.StationRepository;
import com.sustech.cs307.project2.shenzhenmetro.service.LineDetailService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/lineDetails")
@SessionAttributes({"totalStationsToAdd", "stationsAdded"})
public class LineDetailController {
    @Autowired
    private LineDetailRepository lineDetailRepository;

    @Autowired
    private LineRepository lineRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private LineDetailService lineDetailService;

    @GetMapping({"", "/"})
    public String showLineDetailListPage(Model model) {
        List<LineDetail> lineDetails = lineDetailRepository.findAllOrderByLineNumberAndStationOrder();
        model.addAttribute("lineDetails", lineDetails);
        return "lineDetails/index";
    }

    @ModelAttribute("totalStationsToAdd")
    public Integer totalStationsToAdd() {
        return 0;
    }

    @ModelAttribute("stationsAdded")
    public Integer stationsAdded() {
        return 0;
    }

    @GetMapping("/create")
    public String showLineDetailCreatePage(@RequestParam(required = false) Integer numStations, Model model,
                                           @SessionAttribute("totalStationsToAdd") Integer totalStationsToAdd,
                                           @SessionAttribute("stationsAdded") Integer stationsAdded) {
        if (numStations != null) {
            model.addAttribute("totalStationsToAdd", numStations);
            model.addAttribute("stationsAdded", 0);
        } else {
            model.addAttribute("totalStationsToAdd", totalStationsToAdd);
            model.addAttribute("stationsAdded", stationsAdded);
        }

        LineDetailDto lineDetailDto = new LineDetailDto();
        model.addAttribute("lineDetailDto", lineDetailDto);
        return "lineDetails/create_line_detail";
    }

    @Transactional
    @PostMapping("/create")
    public String createLineDetail(@Valid @ModelAttribute LineDetailDto lineDetailDto, BindingResult bindingResult,
                                   @ModelAttribute("totalStationsToAdd") Integer totalStationsToAdd,
                                   @ModelAttribute("stationsAdded") Integer stationsAdded,
                                   Model model, SessionStatus sessionStatus) {
        if (bindingResult.hasErrors()) {
            return "lineDetails/create_line_detail";
        }

        String lineName = lineDetailDto.getLineName();
        String stationName = lineDetailDto.getStationName();
        int stationOrder = lineDetailDto.getStationOrder();

        Optional<LineDetail> existingStation = lineDetailRepository.findByLineNameAndStationName(lineName, stationName);
        if (existingStation.isPresent()) {
            bindingResult.addError(new FieldError("lineDetailDto", "stationName", "This station already exists for the specified line."));
        }

        if (lineRepository.findByLineName(lineName) == null) {
            bindingResult.addError(new FieldError("lineDetailDto", "lineName", "Line not found."));
        }

        if (stationRepository.findById(stationName).isEmpty()) {
            bindingResult.addError(new FieldError("lineDetailDto", "stationName", "Station not found."));
        }

        if (bindingResult.hasErrors()) {
            return "lineDetails/create_line_detail";
        }

        LineDetail lineDetail = new LineDetail();
        lineDetail.setLineName(lineDetailDto.getLineName());
        lineDetail.setStationName(lineDetailDto.getStationName());
        lineDetail.setStationOrder(lineDetailDto.getStationOrder());
        lineDetailRepository.updateStationBeforeCreate(lineName, stationOrder);
        lineDetailRepository.save(lineDetail);

        stationsAdded++;
        model.addAttribute("stationsAdded", stationsAdded);

        if (stationsAdded >= totalStationsToAdd) {
            sessionStatus.setComplete();
            return "redirect:/lineDetails";
        }

        return "redirect:/lineDetails/create";
    }

    @Transactional
    @GetMapping("/remove")
    public String removeStationFromLineDetail(@RequestParam int id, Model model) {
        LineDetail lineDetail = lineDetailRepository.findById(id).orElse(null);
        if (lineDetail != null) {
            String lineName = lineDetail.getLineName();
            int stationOrder = lineDetail.getStationOrder();
            lineDetailRepository.delete(lineDetail);
            lineDetailRepository.updateStationOrderAfterDelete(lineName, stationOrder);
            model.addAttribute("successMessage", "Station removed successfully.");
        } else {
            model.addAttribute("errorMessage", "Station not found.");
        }

        List<LineDetail> lineDetails = lineDetailRepository.findAll();
        model.addAttribute("lineDetails", lineDetails);
        return "redirect:/lineDetails";
    }

    @GetMapping("/search")
    public String showSearchStationPage(Model model) {
        LineDetailSearchDto lineDetailSearchDto = new LineDetailSearchDto();
        model.addAttribute("lineDetailSearchDto", lineDetailSearchDto);
        return "lineDetails/search_line_detail";
    }

    @PostMapping("/search")
    public String searchStation(@Valid @ModelAttribute LineDetailSearchDto lineDetailSearchDto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "lineDetails/search_line_detail";
        }

        String lineName = lineDetailSearchDto.getLineName();
        String stationName = lineDetailSearchDto.getStationName();
        int offset = lineDetailSearchDto.getOffset();

        boolean lineExists = lineDetailRepository.existsByLineName(lineName);
        if (!lineExists) {
            bindingResult.addError(new FieldError("lineDetailDto", "lineName", "Line not found."));
        }

        Optional<LineDetail> currentStation = lineDetailRepository.findByLineNameAndStationName(lineName, stationName);
        if (currentStation.isEmpty()) {
            bindingResult.addError(new FieldError("lineDetailDto", "stationName", "Station not found on the specified line."));
        }

        if (bindingResult.hasErrors()) {
            return "lineDetails/search_line_detail";
        }

        int currentOrder = currentStation.get().getStationOrder();
        int targetOrder = currentOrder + offset;

        Optional<LineDetail> targetStation = lineDetailRepository.findByLineNameAndStationOrder(lineName, targetOrder);
        if (targetStation.isEmpty()) {
            bindingResult.addError(new FieldError("lineDetailDto", "offset", "No station found at the specified offset."));
            return "lineDetails/search_line_detail";
        }

        model.addAttribute("targetStation", targetStation.get());
        return "lineDetails/search_line_detail";
    }

    @GetMapping("/navigate")
    public String showNavigateRoutesPage(Model model) {
        LineDetailNavigateDto lineDetailNavigateDto = new LineDetailNavigateDto();
        model.addAttribute("lineDetailNavigateDto", lineDetailNavigateDto);
        return "lineDetails/navigate_routes";
    }

    @PostMapping("/navigate")
    public String navigateRoutes(@Valid @ModelAttribute LineDetailNavigateDto lineDetailNavigateDto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "lineDetails/navigate_routes";
        }

        String startStation = lineDetailNavigateDto.getStartStation();
        String endStation = lineDetailNavigateDto.getEndStation();

        if (stationRepository.findById(startStation).isEmpty()) {
            bindingResult.addError(new FieldError("lineDetailNavigateDto", "startStation", "Station not found."));
        }

        if (stationRepository.findById(endStation).isEmpty()) {
            bindingResult.addError(new FieldError("lineDetailNavigateDto", "endStation", "Station not found."));
        }

        if (startStation.equals(endStation)) {
            bindingResult.addError(new FieldError("lineDetailNavigateDto", "endStation", "End station should be different from the start station."));
        }

        if (bindingResult.hasErrors()) {
            return "lineDetails/navigate_routes";
        }

        List<LineDetail> lineDetails = lineDetailRepository.findAll();
        List<List<String>> paths = lineDetailService.findAllPaths(lineDetails, startStation, endStation);

        model.addAttribute("paths", paths);
        return "lineDetails/navigate_routes";
    }
}
