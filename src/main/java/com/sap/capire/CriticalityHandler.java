package com.sap.capire;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsEnumType;
import com.sap.cds.reflect.CdsType;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.ServiceName;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@ServiceName(value = "*", type = ApplicationService.class)
public class CriticalityHandler implements EventHandler {

    private final Map<String, Integer> criticalityMap;

    public CriticalityHandler() {
        this.criticalityMap = new HashMap<>();
        this.criticalityMap.put("criticality.VeryNegative", -1);
        this.criticalityMap.put("criticality.Neutral", 0);
        this.criticalityMap.put("criticality.Negative", 1);
        this.criticalityMap.put("criticality.Critical", 2);
        this.criticalityMap.put("criticality.Positive", 3);
        this.criticalityMap.put("criticality.VeryPositive", 4);
        this.criticalityMap.put("criticality.Information", 5);
    }

    @After
    public void handleCriticality(CdsReadEventContext ctx) {

        Optional<CdsEntity> anyEntity = ctx.getModel().entities().filter(
                cdsEntity -> cdsEntity.getQualifiedName().equalsIgnoreCase(ctx.getTarget().getQualifiedName())
        ).findFirst();

        if (anyEntity.isPresent()) {
            anyEntity.get().elements().filter(
                    cdsElement -> cdsElement.getType().isEnum()
            ).findFirst().ifPresent(cdsElement -> processResultForCriticalityAnnotatedElement(cdsElement, ctx.getResult()));
        }

    }

    private void processResultForCriticalityAnnotatedElement(CdsElement cdsElement, Result result) {
        for (Row row : result.list()) {
            if (row.containsKey(cdsElement.getName())) {
                Map<String, Integer> criticalityValues = getCriticalityValues(cdsElement.getType());
                if (!criticalityValues.isEmpty()) {
                    String value = (String) row.get(cdsElement.getName());
                    if (row.containsKey("criticality") && criticalityValues.containsKey(value)) {
                        row.put("criticality", criticalityValues.get(value));
                    }
                }
            }
        }

    }

    private Map<String, Integer> getCriticalityValues(CdsType type) {
        final Map<String, Integer> valueCriticalityMap = new HashMap<>();
        Collection<CdsEnumType.Enumeral<String>> values = ((CdsEnumType<String>) type).enumerals().values();

        for (CdsEnumType.Enumeral<String> value : values) {
            value.annotations().filter(cdsAnnotation -> cdsAnnotation.getName().startsWith("criticality.")).findFirst().ifPresent(cdsAnnotation ->
                    valueCriticalityMap.put(value.value(), this.criticalityMap.get(cdsAnnotation.getName()))
            );
        }
        return valueCriticalityMap;
    }


}
