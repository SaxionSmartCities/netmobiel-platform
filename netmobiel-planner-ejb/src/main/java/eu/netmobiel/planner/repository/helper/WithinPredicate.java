package eu.netmobiel.planner.repository.helper;

import java.io.Serializable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.expression.LiteralExpression;
import org.hibernate.query.criteria.internal.predicate.AbstractSimplePredicate;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class WithinPredicate extends AbstractSimplePredicate implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 7448395254341006070L;
	private final Expression<Point> matchExpression;
    private final Expression<Geometry> area;

    public WithinPredicate(CriteriaBuilder criteriaBuilder, Expression<Point> matchExpression, Geometry area) {
        this(criteriaBuilder, matchExpression, new LiteralExpression<Geometry>((CriteriaBuilderImpl)criteriaBuilder, area));
    }
    public WithinPredicate(CriteriaBuilder criteriaBuilder, Expression<Point> matchExpression, Expression<Geometry> area) {
        super((CriteriaBuilderImpl)criteriaBuilder);
        this.matchExpression = matchExpression;
        this.area = area;
    }

    public Expression<Point> getMatchExpression() {
        return matchExpression;
    }

    public Expression<Geometry> getArea() {
        return area;
    }

    public void registerParameters(ParameterRegistry registry) {
        // Nothing to register
    }

    @Override
    public String render(boolean isNegated, RenderingContext renderingContext) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(" within(")
                .append(((Renderable) getMatchExpression()).render(renderingContext))
                .append(", ")
                .append(((Renderable) getArea()).render(renderingContext))
                .append(") = true ");
        return buffer.toString();
    }
}