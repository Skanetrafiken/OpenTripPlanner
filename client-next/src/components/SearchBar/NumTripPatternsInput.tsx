import { Form } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';

export function NumTripPatternsInput({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  return (
    <Form.Group>
      <Form.Label column="sm" htmlFor="searchWindowInput">
        Number of trip patterns
      </Form.Label>
      <Form.Control
        type="number"
        id="searchWindowInput"
        size="sm"
        placeholder="12"
        min={1}
        value={tripQueryVariables.numTripPatterns || ''}
        onChange={(event) =>
          setTripQueryVariables({
            ...tripQueryVariables,
            numTripPatterns: Number(event.target.value) > 0 ? Number(event.target.value) : undefined,
          })
        }
      />
    </Form.Group>
  );
}
