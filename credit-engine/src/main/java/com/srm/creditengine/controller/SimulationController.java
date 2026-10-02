package com.srm.creditengine.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.srm.creditengine.dto.request.SimulationRequest;
import com.srm.creditengine.dto.response.PricingSimulationResponse;
import com.srm.creditengine.service.SimulationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoint de simulação de precificação.
 *
 * POST /api/simulate
 *
 * Calcula o valor presente em tempo real sem persistir nada. Usado pelo painel
 * do operador antes de confirmar a liquidação.
 */
@RestController
@RequestMapping("/api/simulate")
@Tag(name = "Simulation", description = "Simulação de precificação em tempo real")
public class SimulationController {

	private final SimulationService simulationService;

	public SimulationController(SimulationService simulationService) {
		this.simulationService = simulationService;
	}

	@PostMapping
	@Operation(summary = "Simular precificação", description = """
			Calcula o valor presente de um recebível em tempo real sem persistir nada.
			Retorna VP em BRL, valor final na moeda de pagamento, deságio e parâmetros usados.
			Para moeda USD, usa a taxa de câmbio vigente no momento da consulta.
			""")
	public PricingSimulationResponse simulate(@Valid @RequestBody SimulationRequest request) {
		return simulationService.simulate(request);
	}
}
