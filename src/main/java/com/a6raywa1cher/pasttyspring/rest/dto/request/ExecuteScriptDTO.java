package com.a6raywa1cher.pasttyspring.rest.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ExecuteScriptDTO {
	@NotNull
	private String stdin;
}
