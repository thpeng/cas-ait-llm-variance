package ch.thp.mas.llm.variance.plan;

public class PlanException extends RuntimeException {

    public PlanException(String message) {
        super(message);
    }

    public PlanException(String message, Throwable cause) {
        super(message, cause);
    }
}
