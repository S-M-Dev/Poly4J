package pro.smdev.poly4j.core;

public record HttpResponse<T>(T body, int status) {
}
