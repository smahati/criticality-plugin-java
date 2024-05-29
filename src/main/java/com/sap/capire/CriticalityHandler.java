package com.sap.capire;

import com.sap.cds.CdsData;
import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.reflect.CdsAssociationType;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;


@ServiceName(value = "*", type = ApplicationService.class)
public class CriticalityHandler implements EventHandler {

    public static final String CRITICALITY_ELEMENT = "criticality";
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

        ctx.getModel().entities().filter(
                cdsEntity -> cdsEntity.getQualifiedName().equalsIgnoreCase(ctx.getTarget().getQualifiedName())
        ).findFirst().ifPresent(cdsEntity -> cdsEntity.elements()
                .filter(cdsElement -> cdsElement.getType().isEnum() || cdsElement.getType().isAssociation())
                .forEach(cdsElement -> processResultForCriticalityAnnotatedElement(cdsElement, ctx.getResult())));
    }

    private void processResultForCriticalityAnnotatedElement(CdsElement cdsElement, Result result) {
        for (Row row : result.list()) {
            processRow(cdsElement, row);
        }
    }

    private void processRow(CdsElement cdsElement, CdsData row) {
        if (cdsElement.getType().isEnum()) {
            handleEnumElement(cdsElement, row);
        } else if (cdsElement.getType().isAssociation() && row.containsKey(cdsElement.getName())) {
            handleExpandedEnumAssociation(cdsElement, row);
        }

    }

    @SuppressWarnings("unchecked")
    private void handleExpandedEnumAssociation(CdsElement cdsElement, CdsData row) {
        ((CdsAssociationType) cdsElement.getType()).getTarget().elements().filter(innerCdsElement -> innerCdsElement.getType().isEnum()).findFirst().ifPresent(innerCdsElement -> {
            if (isToManyAssoc(cdsElement)) {
                ((List<CdsData>) row.get(cdsElement.getName())).forEach(innerRow -> processRow(innerCdsElement, innerRow));
            }
            else {
                processRow(innerCdsElement, (CdsData) row.get(cdsElement.getName()));
            }
        });
    }

    private void handleEnumElement(CdsElement cdsElement, CdsData row) {
        if (row.containsKey(cdsElement.getName())) {
            Map<String, Integer> criticalityValues = getCriticalityValues(cdsElement.getType());
            if (!criticalityValues.isEmpty()) {
                String value = (String) row.get(cdsElement.getName());
                if (criticalityValues.containsKey(value)) {
                    row.put(CRITICALITY_ELEMENT, criticalityValues.get(value));
                }
            }
        }
    }

    private static boolean isToManyAssoc(CdsElement cdsElement) {
        return ((CdsAssociationType) cdsElement.getType()).getCardinality().getTargetMax().equals("*");
    }

    @SuppressWarnings("unchecked")
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
